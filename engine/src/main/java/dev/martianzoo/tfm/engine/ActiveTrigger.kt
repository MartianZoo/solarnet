package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.ChangeEvent
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
        is ByTrigger -> ByDoer(from(trigger.inner), trigger.by)
        is WhenGain -> Self(true)
        is WhenRemove -> Self(false)
        is OnGainOf -> OnChange(trigger.expression, gaining = true)
        is OnRemoveOf -> OnChange(trigger.expression, gaining = false)
        is Transform -> error("should have been transformed by now")
      }
    }
  }

  data class Hit(val count: Int, val fixer: (Instruction) -> Instruction = { it })

  abstract fun matchSelf(triggerEvent: ChangeEvent, game: Game): Hit?

  abstract fun matchOther(triggerEvent: ChangeEvent, game: Game): Hit?

  data class ByDoer(val inner: ActiveTrigger, val by: ClassName) : ActiveTrigger() {

    override fun matchSelf(triggerEvent: ChangeEvent, game: Game): Hit? {
      val contextP: ClassName? = triggerEvent.cause?.doer
      if (isPlayerSpecific() && contextP != by) return null

      val hit = inner.matchSelf(triggerEvent, game) ?: return null

      return if (by == OWNER) {
        hit.copy { ReplaceOwnerWith(contextP).transform(hit.fixer(it)) }
      } else {
        hit
      }
    }

    override fun matchOther(triggerEvent: ChangeEvent, game: Game): Hit? {
      val contextP: ClassName? =
          triggerEvent.cause?.let { game.toComponent(it.contextComponent).owner() }
      if (isPlayerSpecific() && contextP != by) return null

      val hit = inner.matchOther(triggerEvent, game) ?: return null

      return if (by == OWNER) {
        hit.copy { ReplaceOwnerWith(contextP).transform(hit.fixer(it)) }
      } else {
        hit
      }
    }
    fun isPlayerSpecific() = by.toString().startsWith("Player")
  }

  data class OnChange(val match: Expression, val gaining: Boolean) : ActiveTrigger() {

    // It should not need to match itself since it will already be included in the sweep
    override fun matchSelf(triggerEvent: ChangeEvent, game: Game) = null

    override fun matchOther(triggerEvent: ChangeEvent, game: Game): Hit? {
      val expr: Expression? = if (gaining) triggerEvent.change.gaining else triggerEvent.change.removing
      return expr?.let {
        if (game.resolve(it).isSubtypeOf(game.resolve(match))) {
          Hit(triggerEvent.change.count)
        } else null
      }
    }
  }

  data class Self(val isOnGain: Boolean) : ActiveTrigger() {

    override fun matchSelf(triggerEvent: ChangeEvent, game: Game): Hit? {
      val isThisAGain = (triggerEvent.change.gaining != null)
      return if (isThisAGain == isOnGain) Hit(triggerEvent.change.count) else null
    }

    // This never matches, because an *existing* Foo would trigger on *another* Foo
    override fun matchOther(triggerEvent: ChangeEvent, game: Game) = null
  }
}
