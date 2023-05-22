package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.BasicTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WrappingTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement

private typealias Hit = (Instruction) -> Instruction

/** A triggered effect of "live" component existing in the [ComponentGraph]. */
internal data class ActiveEffect
private constructor(
    private val subscription: Subscription,
    private val automatic: Boolean,
    private val instruction: Instruction,
    private val contextComponent: Expression,
    private val contextOwner: Player?,
) {
  companion object {
    fun from(it: Effect, context: Component) =
        ActiveEffect(
            Subscription.from(it.trigger, context),
            it.automatic,
            it.instruction,
            context.expressionFull,
            context.owner,
        )
  }

  val classToCheck: ClassName? by subscription::classToCheck

  fun onChangeToSelf(triggerEvent: ChangeEvent, game: SnReader) =
      onChange(triggerEvent, game, isSelf = true)

  fun onChangeToOther(triggerEvent: ChangeEvent, game: SnReader) =
      onChange(triggerEvent, game, isSelf = false)

  private fun onChange(triggerEvent: ChangeEvent, reader: SnReader, isSelf: Boolean): Task? {
    val player = contextOwner ?: triggerEvent.owner
    val hit = subscription.checkForHit(triggerEvent, player, isSelf, reader) ?: return null
    val cause = Cause(contextComponent, triggerEvent.ordinal)
    return Task(TaskId("ZZ"), player, automatic, hit(instruction), cause = cause)
  }

  internal sealed class Subscription {
    companion object {
      fun from(
          trigger: Trigger,
          context: Component,
      ): Subscription {
        return when (trigger) {
          is BasicTrigger -> {
            when (trigger) {
              is WhenGain -> SelfSubscription(context, matchOnGain = true)
              is WhenRemove -> SelfSubscription(context, matchOnGain = false)
              is OnGainOf -> RegularSubscription(trigger.expression, matchOnGain = true)
              is OnRemoveOf -> RegularSubscription(trigger.expression, matchOnGain = false)
            }
          }
          is WrappingTrigger -> {
            val inner = from(trigger.inner, context)
            when (trigger) {
              is ByTrigger -> PersonalSubscription(inner, trigger.by)
              is IfTrigger -> ConditionalSubscription(inner, trigger.condition)
              is XTrigger -> UnscaledSubscription(inner)
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
        game: SnReader,
    ): Hit?

    abstract val classToCheck: ClassName?
  }

  private data class ConditionalSubscription(
      val inner: Subscription,
      val condition: Requirement,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: SnReader,
    ): Hit? {
      val wouldHit = inner.checkForHit(currentEvent, actor, isSelf, game) ?: return null
      return if (game.evaluate(condition)) wouldHit else null
    }

    override val classToCheck = inner.classToCheck
  }

  private data class UnscaledSubscription(val inner: Subscription) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: SnReader,
    ): Hit? {
      // just fake it like only one happened
      return inner.checkForHit(
          currentEvent.copy(change = currentEvent.change.copy(count = 1)), actor, isSelf, game)
    }

    override val classToCheck = inner.classToCheck
  }

  private data class PersonalSubscription(
      val inner: Subscription,
      val by: ClassName,
  ) : Subscription() {
    val player: Player? =
        try {
          Player(by)
        } catch (e: Exception) {
          require(by == OWNER || by == ANYONE)
          null
        }

    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: SnReader,
    ): Hit? {
      if (player != null && actor != player) return null
      val originalHit = inner.checkForHit(currentEvent, actor, isSelf, game) ?: return null

      return if (by == OWNER) {
        { replaceOwnerWith(actor).transform(originalHit(it)) }
      } else {
        originalHit
      }
    }

    override val classToCheck = inner.classToCheck
  }

  private data class SelfSubscription(
      val context: Component,
      val matchOnGain: Boolean,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: SnReader,
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

  private data class RegularSubscription(
      val match: Expression,
      val matchOnGain: Boolean,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: SnReader,
    ): Hit? {
      if (isSelf) return null
      val change = currentEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
      // Will be refinement-aware (#48)
      val changeType = game.resolve(expr)
      val matchType = game.resolve(match)
      return if (changeType.narrows(matchType, game)) {
        val subber = game.transformers.substituter(matchType, changeType)
        val h: Hit = { subber.transform(it) * change.count }
        h
      } else {
        null
      }
    }

    override val classToCheck = match.className
  }
}
