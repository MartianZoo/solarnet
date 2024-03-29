package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player
import dev.martianzoo.data.Task
import dev.martianzoo.engine.Engine.GameScoped
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
import javax.inject.Inject
import javax.inject.Provider

@GameScoped
internal class Effector @Inject constructor(reader: Provider<GameReader>?) {
  private val reader: GameReader by lazy { reader!!.get() }
  private val registry = HashMultiset<ActiveEffect>()

  private val effects = mutableMapOf<Component, List<ActiveEffect>>()

  internal fun add(component: Component, delta: Int) =
      activeEffects(component).forEach { registry.add(it, delta) }

  internal fun mustRemove(component: Component, delta: Int) =
      activeEffects(component).forEach { registry.mustRemove(it, delta) }

  private fun activeEffects(component: Component): List<ActiveEffect> {
    fun activeEffect(fx: Effect) =
        ActiveEffect(
            Subscription.from(fx.trigger, component), fx.automatic, fx.instruction, component)

    return effects.computeIfAbsent(component) { it.effects.map(::activeEffect) }
  }

  fun fire(triggerEvent: ChangeEvent, automatic: Boolean? = null): List<Task> =
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
      val player = context.owner ?: triggerEvent.owner
      val hit = subscription.checkForHit(triggerEvent, player, isSelf, reader) ?: return null
      val cause = Cause(context.expression, triggerEvent.ordinal)
      return Task.noid(player, automatic, hit(instruction), cause = cause)
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
        actor: Player,
        isSelf: Boolean,
        reader: GameReader,
    ): Hit?

    internal abstract val classToCheck: ClassName?

    private data class Regular(val match: Expression, val matchOnGain: Boolean) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Player,
          isSelf: Boolean,
          reader: GameReader
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
          val h: Hit = { subber.transform(it) * change.count }
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
          actor: Player,
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
      val player: Player? = if (by == OWNER || by == ANYONE) null else Player(by)

      override fun checkForHit(
          currentEvent: ChangeEvent,
          actor: Player,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        if (player != null && actor != player) return null
        val originalHit = inner.checkForHit(currentEvent, actor, isSelf, reader) ?: return null

        return if (by == OWNER) {
          { replaceOwnerWith(actor).transform(originalHit(it)) }
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
          actor: Player,
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
          actor: Player,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        // just fake it like only one happened
        return inner.checkForHit(
            currentEvent.copy(change = currentEvent.change.copy(count = 1)), actor, isSelf, reader)
      }

      override val classToCheck = inner.classToCheck
    }
  }
}

private typealias Hit = (Instruction) -> Instruction
