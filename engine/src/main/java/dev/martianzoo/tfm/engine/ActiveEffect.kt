package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
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
internal data class ActiveEffect(
    private val context: Component,
    private val subscription: Subscription,
    private val automatic: Boolean,
    private val instruction: Instruction,
) {
  companion object {
    fun from(it: Effect, context: Component) =
        ActiveEffect(context, Subscription.from(it.trigger, context), it.automatic, it.instruction)
  }

  operator fun times(multiplier: Int) = copy(instruction = instruction * multiplier)

  fun onChangeToSelf(triggerEvent: ChangeEvent, game: Game) =
      onChange(triggerEvent, game, isSelf = true)

  fun onChangeToOther(triggerEvent: ChangeEvent, game: Game) =
      onChange(triggerEvent, game, isSelf = false)

  private fun onChange(triggerEvent: ChangeEvent, game: Game, isSelf: Boolean): FiredEffect? {
    val player = context.owner() ?: triggerEvent.owner
    val hit = subscription.checkForHit(triggerEvent, player, isSelf, game) ?: return null
    val cause = Cause(context.expression, triggerEvent.ordinal)
    return FiredEffect(hit(instruction), player, cause, automatic)
  }

  internal data class FiredEffect(
      val instruction: Instruction,
      val player: Player,
      val cause: Cause,
      val automatic: Boolean,
  ) {
    operator fun times(factor: Int) = copy(instruction = instruction * factor)
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
              is XTrigger -> UnscaledSubscription(inner) // TODO flexible?
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
        game: Game,
    ): Hit?
  }

  private data class ConditionalSubscription(
      val inner: Subscription,
      val condition: Requirement,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: Game,
    ): Hit? {
      // This sort of feels out of order, but I don't think that hurts anything
      return if (game.reader.evaluate(condition)) {
        inner.checkForHit(currentEvent, actor, isSelf, game)
      } else {
        null
      }
    }
  }

  private data class UnscaledSubscription(val inner: Subscription) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: Game,
    ): Hit? {
      // just fake it like only one happened
      return inner.checkForHit(
          currentEvent.copy(change = currentEvent.change.copy(count = 1)), actor, isSelf, game)
    }
  }

  private data class PersonalSubscription(
      val inner: Subscription,
      val by: ClassName,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: Game,
    ): Hit? {
      if (isPlayerSpecificTrigger() && actor.className != by) return null

      val originalHit = inner.checkForHit(currentEvent, actor, isSelf, game) ?: return null

      return if (by == OWNER) {
        { replaceOwnerWith(actor).transform(originalHit(it)) }
      } else {
        originalHit
      }
    }

    fun isPlayerSpecificTrigger(): Boolean {
      if (by.toString().matches(Regex("^Player[1-5]$"))) return true
      require(by == ANYONE || by == OWNER) { by }
      return false
    }
  }

  private data class SelfSubscription(
      val context: Component,
      val matchOnGain: Boolean,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: Game,
    ): Hit? {
      if (!isSelf) return null
      val change = currentEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null

      return if (context.hasType(game.resolve(expr))) { // TODO why not exact, anyway?
        val hit: Hit = { it * currentEvent.change.count }
        hit
      } else {
        null
      }
    }
  }

  private data class RegularSubscription(
      val match: Expression,
      val matchOnGain: Boolean,
  ) : Subscription() {
    override fun checkForHit(
        currentEvent: ChangeEvent,
        actor: Player,
        isSelf: Boolean,
        game: Game,
    ): Hit? {
      if (isSelf) return null
      val change = currentEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
      // Will be refinement-aware (#48)
      val changeType = game.resolve(expr)
      val matchType = game.resolve(match)
      return if (changeType.isSubtypeOf(matchType)) {
        val subber = game.transformers.substituter(matchType, changeType)
        val h: Hit = { subber.transform(it) * change.count }
        h
      } else {
        null
      }
    }
  }
}
