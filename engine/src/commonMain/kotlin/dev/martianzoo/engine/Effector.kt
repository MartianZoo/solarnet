package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.PLAYER
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player
import dev.martianzoo.data.Task
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.BasicTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.pets.ast.Effect.Trigger.WrappingTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.MType
import dev.martianzoo.util.HashMultiset

internal class Effector(readerProvider: Lazy<GameReader>? = null) {
  private val reader: GameReader by lazy { readerProvider!!.value }
  private val registry = HashMultiset<ActiveEffect>()

  private val effects = mutableMapOf<Component, List<ActiveEffect>>()

  internal fun add(component: Component, delta: Int) =
      activeEffects(component).forEach { registry.add(it, delta) }

  internal fun mustRemove(component: Component, delta: Int) =
      activeEffects(component).forEach { registry.mustRemove(it, delta) }

  private fun activeEffects(component: Component): List<ActiveEffect> {
    fun activeEffect(fx: Effect) =
        ActiveEffect(
            Subscription.from(fx.trigger, component),
            fx.automatic,
            fx.instruction,
            component,
        )

    return effects.getOrPut(component) { component.effects.map(::activeEffect) }
  }

  internal fun fire(triggerEvent: ChangeEvent, automatic: Boolean? = null): List<Task> =
      fireSelfEffects(triggerEvent, automatic) + fireOtherEffects(triggerEvent, automatic)

  private fun fireSelfEffects(triggerEvent: ChangeEvent, automatic: Boolean? = null): List<Task> =
      listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
          .map(reader::resolve)
          .map { (it as MType).toComponent() }
          .flatMap { activeEffects(it) }
          .filter { automatic == null || it.automatic == automatic }
          .mapNotNull { it.onChangeToSelf(triggerEvent, reader) }

  private fun fireOtherEffects(triggerEvent: ChangeEvent, automatic: Boolean? = null): List<Task> =
      registry.entries
          .filter { (fx, _) -> automatic == null || fx.automatic == automatic }
          .mapNotNull { (fx, ct) -> fx.onChangeToOther(triggerEvent, reader)?.times(ct) }

  private data class ActiveEffect(
      private val subscription: Subscription,
      internal val automatic: Boolean,
      private val instruction: Instruction,
      private val context: Component,
  ) {
    fun onChangeToSelf(triggerEvent: ChangeEvent, reader: GameReader) =
        onChange(triggerEvent, reader, isSelf = true)

    fun onChangeToOther(triggerEvent: ChangeEvent, reader: GameReader) =
        onChange(triggerEvent, reader, isSelf = false)

    private fun onChange(triggerEvent: ChangeEvent, reader: GameReader, isSelf: Boolean): Task? {
      val taskActor = actorForTriggeredWork(triggerEvent, reader)
      val hit = subscription.checkForHit(triggerEvent, taskActor, isSelf, reader) ?: return null
      val cause = Cause(context.expression, triggerEvent.ordinal)
      val triggeredBy = triggerEvent.actor.takeUnless { it == taskActor }
      return Task.noid(
          taskActor,
          automatic,
          hit(instruction),
          cause = cause,
          triggeredBy = triggeredBy,
      )
    }

    /**
     * The compatibility rule for choosing the Actor associated with work produced by an effect.
     * `BY` currently tests this Actor as well, although routing and trigger matching may eventually
     * need distinct inputs.
     *
     * Automatic effects are represented temporarily as Tasks but execute inline through the
     * triggering Actor's Instructor and Changer, so their resulting ChangeEvents retain that Actor.
     */
    private fun actorForTriggeredWork(triggerEvent: ChangeEvent, reader: GameReader): Actor =
        context.owner ?: changedComponentOwner(triggerEvent, reader) ?: triggerEvent.actor

    private fun changedComponentOwner(triggerEvent: ChangeEvent, reader: GameReader): Player? {
      val expression = triggerEvent.change.gaining ?: triggerEvent.change.removing ?: return null
      return (reader.resolve(expression) as MType).toComponent().owner
    }
  }

  private sealed class Subscription {
    companion object {
      fun from(trigger: Trigger, context: Component): Subscription {
        return when (trigger) {
          is BasicTrigger -> {
            when (trigger) {
              is WhenGain -> Self(context, matchOnGain = true)
              is WhenRemove -> Self(context, matchOnGain = false)
              is OnGainOf -> Regular(trigger.expression, matchOnGain = true)
              is OnRemoveOf -> Regular(trigger.expression, matchOnGain = false)
            }
          }
          is WrappingTrigger -> {
            val inner = from(trigger.inner, context)
            when (trigger) {
              is ByTrigger -> Personal(inner, trigger.by)
              is IfTrigger -> Conditional(inner, trigger.condition)
              is XTrigger -> Unscaled(inner)
              is Transform -> error("should have been transformed by now: $trigger")
            }
          }
        }
      }
    }

    abstract fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Actor,
        isSelf: Boolean,
        reader: GameReader,
    ): Hit?

    internal abstract val classToCheck: ClassName?

    private data class Regular(val match: Expression, val matchOnGain: Boolean) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Actor,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        reader as GameReaderImpl
        if (isSelf) return null
        val change = currentEvent.change
        val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
        // Will be refinement-aware (#48)
        val changeType = reader.resolve(expr)
        val matchType = reader.resolve(match)
        return if (changeType.narrows(matchType, reader)) {
          val subber = reader.transformers.substituter(matchType, changeType)
          // TODO: Reconsider this substitution when Actor becomes a distinct runtime concept.
          // Resolving a Player-bounded expression such as UseAction1<Owner, Foo> correctly
          // intersects its type to UseAction1<Player, Foo>. Keep the original Owner token's other
          // role as a contextual variable: instructions emitted by the hit still belong to the
          // concrete actor that matched the trigger.
          val contextualOwner =
              if (OWNER in match) (actor as? Player)?.let(::replaceOwnerWith) else null
          val h: Hit = {
            val substituted = subber.transform(it)
            (contextualOwner?.transform(substituted) ?: substituted) * change.count
          }
          h
        } else {
          null
        }
      }

      override val classToCheck = match.className
    }

    private data class Self(val context: Component, val matchOnGain: Boolean) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Actor,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {

        if (!isSelf) return null
        val change = currentEvent.change
        val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null

        return if (expr == context.expressionFull) {
          val hit: Hit = { it * currentEvent.change.count }
          hit
        } else {
          null
        }
      }

      override val classToCheck = null
    }

    private data class Personal(val inner: Subscription, val by: ClassName) : Subscription() {
      val specificActor: Actor? =
          if (by == OWNER || by == ANYONE || by == PLAYER) null else Actor.from(by)

      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Actor,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        if (specificActor != null && actor != specificActor) return null
        if (by == PLAYER && actor !is Player) return null
        val originalHit = inner.checkForHit(currentEvent, actor, isSelf, reader) ?: return null

        return if (by == OWNER) {
          val owner = actor as? Player ?: return null
          { replaceOwnerWith(owner).transform(originalHit(it)) }
        } else {
          originalHit
        }
      }

      override val classToCheck = inner.classToCheck
    }

    private data class Conditional(val inner: Subscription, val condition: Requirement) :
        Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Actor,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        val wouldHit = inner.checkForHit(currentEvent, actor, isSelf, reader) ?: return null
        return if (reader.has(condition)) wouldHit else null
      }

      override val classToCheck = inner.classToCheck
    }

    private data class Unscaled(val inner: Subscription) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Actor,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        // just fake it like only one happened
        return inner.checkForHit(
            currentEvent.copy(change = currentEvent.change.copy(count = 1)),
            actor,
            isSelf,
            reader,
        )
      }

      override val classToCheck = inner.classToCheck
    }
  }
}

private typealias Hit = (Instruction) -> Instruction
