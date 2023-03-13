package dev.martianzoo.tfm.engine

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
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.Transformers.ReplaceOwnerWith

sealed class ActiveTrigger {
  companion object {
    fun from(trigger: Trigger): ActiveTrigger {
      return when (trigger) {
        is ByTrigger -> ByActor(from(trigger.inner), trigger.by)
        is IfTrigger -> Conditional(from(trigger.inner), trigger.condition)
        is WhenGain -> MatchOnSelf(true)
        is WhenRemove -> MatchOnSelf(false)
        is OnGainOf -> MatchOnOthers(trigger.expression, matchOnGain = true)
        is OnRemoveOf -> MatchOnOthers(trigger.expression, matchOnGain = false)
        is Transform -> error("should have been transformed by now")
      }
    }
  }

  class Conditional(val inner: ActiveTrigger, val condition: Requirement) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, game: Game, isSelf: Boolean): Hit? {
      return if (game.evaluate(condition)) {
        inner.match(triggerEvent, actor, game, isSelf)
      } else {
        null
      }
    }
  }

  abstract fun match(triggerEvent: ChangeEvent, actor: Actor, game: Game, isSelf: Boolean): Hit?

  data class ByActor(val inner: ActiveTrigger, val by: ClassName) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, game: Game, isSelf: Boolean): Hit? {
      if (isPlayerSpecificTrigger() && actor.className != by) return null

      val originalHit = inner.match(triggerEvent, actor, game, isSelf) ?: return null

      return if (by == OWNER) {
        object : Hit {
          override fun modify(instruction: Instruction): Instruction {
            // TODO which way
            return ReplaceOwnerWith(actor.className).transform(originalHit.modify(instruction))
          }
        }
      } else {
        originalHit
      }
    }

    fun isPlayerSpecificTrigger() = by.toString().startsWith("Player")
  }

  data class MatchOnSelf(val matchOnGain: Boolean) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, game: Game, isSelf: Boolean): Hit? {
      if (!isSelf) return null
      val isThisAGain = (triggerEvent.change.gaining != null)
      return if (isThisAGain == matchOnGain) {
        genericHit(triggerEvent.change.count)
      } else {
        null
      }
    }
  }

  data class MatchOnOthers(val match: Expression, val matchOnGain: Boolean) : ActiveTrigger() {
    override fun match(triggerEvent: ChangeEvent, actor: Actor, game: Game, isSelf: Boolean): Hit? {
      if (isSelf) return null
      val change = triggerEvent.change
      val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
      return if (game.resolve(expr).isSubtypeOf(game.resolve(match))) {
        genericHit(change.count)
      } else {
        null
      }
    }
  }

  interface Hit {
    fun modify(instruction: Instruction): Instruction
  }

  fun genericHit(count: Int): Hit =
      object : Hit {
        override fun modify(instruction: Instruction): Instruction {
          return instruction * count
        }
      }
}
