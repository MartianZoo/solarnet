package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.pets.ast.ClassName
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
import dev.martianzoo.tfm.types.Transformers.ReplaceOwnerWith
import dev.martianzoo.tfm.types.findSubstitutions

typealias Hit = (Instruction) -> Instruction

sealed class ActiveTrigger {
  companion object {
    fun from(
        trigger: Trigger,
        context: Component,
        game: Game,
        tlinks: Set<ClassName>,
    ): ActiveTrigger {
      return when (trigger) {
        is ByTrigger -> ByActor(from(trigger.inner, context, game, tlinks), trigger.by)
        is IfTrigger ->
            Conditional(from(trigger.inner, context, game, tlinks), trigger.condition, game)
        is XTrigger -> AnyAmount(from(trigger.inner, context, game, tlinks))
        is WhenGain -> MatchOnSelf(context, matchOnGain = true, game)
        is WhenRemove -> MatchOnSelf(context, matchOnGain = false, game)
        is OnGainOf -> MatchOnOthers(trigger.expression, matchOnGain = true, game, tlinks)
        is OnRemoveOf -> MatchOnOthers(trigger.expression, matchOnGain = false, game, tlinks)
        is Transform -> error("should have been transformed by now")
      }
    }
  }

  class Conditional(val inner: ActiveTrigger, val condition: Requirement, val game: Game) :
      ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, isSelf: Boolean): Hit? {
      // This sort of feels out of order, but I don't think that hurts anything
      return if (this.game.evaluate(condition)) {
        inner.match(triggerEvent, actor, isSelf)
      } else {
        null
      }
    }
  }

  class AnyAmount(val inner: ActiveTrigger) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, isSelf: Boolean): Hit? {
      // just fake it like only one happened
      return inner.match(
          triggerEvent.copy(change = triggerEvent.change.copy(count = 1)), actor, isSelf)
    }
  }

  abstract fun match(triggerEvent: ChangeEvent, actor: Actor, isSelf: Boolean): Hit?

  data class ByActor(val inner: ActiveTrigger, val by: ClassName) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, isSelf: Boolean): Hit? {
      if (isPlayerSpecificTrigger() && actor.className != by) return null

      val originalHit = inner.match(triggerEvent, actor, isSelf) ?: return null

      return if (by == OWNER) {
        { ReplaceOwnerWith(actor.className).transform(originalHit(it)) }
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

  data class MatchOnSelf(val context: Component, val matchOnGain: Boolean, val game: Game) :
      ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, isSelf: Boolean): Hit? {
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

  data class MatchOnOthers(
      val match: Expression,
      val matchOnGain: Boolean,
      val game: Game,
      val tlinks: Set<ClassName>,
  ) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, isSelf: Boolean): Hit? {
      if (isSelf) return null
      val change = triggerEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
      return if (this.game.resolve(expr).isSubtypeOf(this.game.resolve(match))) {
        val subber = Substituter(findSubstitutions(tlinks, match, expr))
        val h: Hit = { subber.transform(it) * change.count }
        h
      } else {
        null
      }
    }
  }
}
