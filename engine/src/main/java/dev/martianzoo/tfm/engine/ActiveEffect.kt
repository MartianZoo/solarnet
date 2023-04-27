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
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.findSubstitutions

private typealias Hit = (Instruction) -> Instruction

/** A triggered effect of "live" component existing in the [ComponentGraph]. */
internal data class ActiveEffect(
    private val context: Component,
    private val trigger: ActiveTrigger,
    private val automatic: Boolean,
    private val instruction: Instruction,
) {
  companion object {
    fun from(it: Effect, context: Component, game: Game, triggerLinkages: Set<ClassName>) =
        ActiveEffect(
            context,
            ActiveTrigger.from(it.trigger, context, game, triggerLinkages),
            it.automatic,
            it.instruction)
  }

  operator fun times(multiplier: Int) = copy(instruction = instruction * multiplier)

  fun onChangeToSelf(triggerEvent: ChangeEvent) = onChange(triggerEvent, isSelf = true)

  fun onChangeToOther(triggerEvent: ChangeEvent) = onChange(triggerEvent, isSelf = false)

  private fun onChange(triggerEvent: ChangeEvent, isSelf: Boolean): FiredEffect? {
    val player = context.owner() ?: triggerEvent.player
    val hit = trigger.match(triggerEvent, player, isSelf) ?: return null
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

  internal sealed class ActiveTrigger {
    companion object {
      fun from(
          trigger: Trigger,
          context: Component,
          game: Game,
          tlinks: Set<ClassName>,
      ): ActiveTrigger {
        return when (trigger) {
          is ByTrigger -> ByPlayer(from(trigger.inner, context, game, tlinks), trigger.by)
          is IfTrigger ->
              Conditional(from(trigger.inner, context, game, tlinks), trigger.condition, game)
          is XTrigger -> AnyAmount(from(trigger.inner, context, game, tlinks))
          is WhenGain -> MatchOnSelf(context, matchOnGain = true, game)
          is WhenRemove -> MatchOnSelf(context, matchOnGain = false, game)
          is OnGainOf -> MatchOnOthers(trigger.expression, matchOnGain = true, game, tlinks)
          is OnRemoveOf -> MatchOnOthers(trigger.expression, matchOnGain = false, game, tlinks)
          is Transform -> error("should have been transformed by now: $trigger")
        }
      }
    }

    abstract fun match(triggerEvent: ChangeEvent, player: Player, isSelf: Boolean): Hit?
  }
  private data class Conditional(
      val inner: ActiveTrigger,
      val condition: Requirement,
      val game: Game
  ) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, player: Player, isSelf: Boolean): Hit? {
      // This sort of feels out of order, but I don't think that hurts anything
      return if (game.reader.evaluate(condition)) {
        inner.match(triggerEvent, player, isSelf)
      } else {
        null
      }
    }
  }

  private data class AnyAmount(val inner: ActiveTrigger) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, player: Player, isSelf: Boolean): Hit? {
      // just fake it like only one happened
      return inner.match(
          triggerEvent.copy(change = triggerEvent.change.copy(count = 1)), player, isSelf)
    }
  }

  private data class ByPlayer(val inner: ActiveTrigger, val by: ClassName) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, player: Player, isSelf: Boolean): Hit? {
      if (isPlayerSpecificTrigger() && player.className != by) return null

      val originalHit = inner.match(triggerEvent, player, isSelf) ?: return null

      return if (by == OWNER) {
        { replaceOwnerWith(player).transform(originalHit(it)) }
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

  private data class MatchOnSelf(val context: Component, val matchOnGain: Boolean, val game: Game) :
      ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, player: Player, isSelf: Boolean): Hit? {
      if (!isSelf) return null
      val change = triggerEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null

      return if (context.hasType(game.resolve(expr))) {
        { it * triggerEvent.change.count }
      } else {
        null
      }
    }
  }

  private data class MatchOnOthers(
      val match: Expression,
      val matchOnGain: Boolean,
      val game: Game,
      val tlinks: Set<ClassName>,
  ) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, player: Player, isSelf: Boolean): Hit? {
      if (isSelf) return null
      val change = triggerEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
      // Will be refinement-aware (#48)
      return if (game.resolve(expr).isSubtypeOf(game.resolve(match))) {
        val subber = Substituter(findSubstitutions(tlinks, match, expr))
        val h: Hit = { subber.transform(it) * change.count }
        h
      } else {
        null
      }
    }
  }
}
