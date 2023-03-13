package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.types.Transformers.ReplaceOwnerWith

sealed class ActiveTrigger {
  companion object {
    fun from(trigger: Trigger): ActiveTrigger {
      return when (trigger) {
        is ByTrigger -> ByActor(from(trigger.inner), trigger.by)
        is WhenGain -> Self(true)
        is WhenRemove -> Self(false)
        is OnGainOf -> OnChange(trigger.expression, matchOnGain = true)
        is OnRemoveOf -> OnChange(trigger.expression, matchOnGain = false)
        is Transform -> error("should have been transformed by now")
      }
    }
  }

  interface Hit {
    val count: Int
    fun modify(instruction: Instruction): Instruction
  }

  abstract fun matchSelf(triggerEvent: ChangeEvent, actor: Actor, game: Game): Hit?

  abstract fun matchOther(triggerEvent: ChangeEvent, actor: Actor, game: Game): Hit?

  data class ByActor(val inner: ActiveTrigger, val by: ClassName) : ActiveTrigger() {

    override fun matchSelf(triggerEvent: ChangeEvent, actor: Actor, game: Game): Hit? {
      if (isPlayerSpecific() && actor.className != by) return null

      val originalHit = inner.matchSelf(triggerEvent, actor, game) ?: return null

      return if (by == OWNER) {
        object : Hit {
          override val count by originalHit::count
          override fun modify(instruction: Instruction): Instruction {
            // TODO which way
            return ReplaceOwnerWith(actor.className).transform(originalHit.modify(instruction))
          }
        }
      } else {
        originalHit
      }
    }

    override fun matchOther(triggerEvent: ChangeEvent, actor: Actor, game: Game): Hit? {
      if (isPlayerSpecific() && actor.className != by) return null

      val originalHit = inner.matchOther(triggerEvent, actor, game) ?: return null

      return if (by == OWNER) {
        object : Hit {
          override val count by originalHit::count
          override fun modify(instruction: Instruction): Instruction {
            // TODO which way
            return ReplaceOwnerWith(actor.className).transform(originalHit.modify(instruction))
          }
        }
      } else {
        originalHit
      }
    }
    fun isPlayerSpecific() = by.toString().startsWith("Player")
  }

  data class OnChange(val match: Expression, val matchOnGain: Boolean) : ActiveTrigger() {

    // It should not need to match itself since it will already be included in the sweep
    override fun matchSelf(triggerEvent: ChangeEvent, actor: Actor, game: Game) = null

    override fun matchOther(triggerEvent: ChangeEvent, actor: Actor, game: Game): Hit? {
      val expr: Expression =
          if (matchOnGain) {
            triggerEvent.change.gaining
          } else {
            triggerEvent.change.removing
          } ?: return null
      return if (game.resolve(expr).isSubtypeOf(game.resolve(match))) {
        genericHit(triggerEvent.change.count)
      } else {
        null
      }
    }
  }

  data class Self(val matchOnGain: Boolean) : ActiveTrigger() {
    override fun matchSelf(triggerEvent: ChangeEvent, actor: Actor, game: Game): Hit? {
      val isThisAGain = (triggerEvent.change.gaining != null)
      return if (isThisAGain == matchOnGain) {
        genericHit(triggerEvent.change.count)
      } else {
        null
      }
    }

    // This never matches, because then an *existing* Foo would trigger on *another* Foo
    override fun matchOther(triggerEvent: ChangeEvent, actor: Actor, game: Game) = null
  }

  fun genericHit(count: Int): Hit =
      object : Hit {
        override val count = count
        override fun modify(instruction: Instruction): Instruction {
          return instruction
        }
      }
}
