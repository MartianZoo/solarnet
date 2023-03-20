package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction

data class ActiveEffect(
    val context: Component,
    val trigger: ActiveTrigger,
    val automatic: Boolean,
    val instruction: Instruction,
) {
  companion object {
    fun from(it: Effect, context: Component, game: Game) =
        ActiveEffect(context,
            ActiveTrigger.from(it.trigger, context, game),
            it.automatic,
            it.instruction)
  }

  fun onChangeToSelf(triggerEvent: ChangeEvent) = onChange(triggerEvent, isSelf = true)

  fun onChangeToOther(triggerEvent: ChangeEvent) = onChange(triggerEvent, isSelf = false)

  private fun onChange(triggerEvent: ChangeEvent, isSelf: Boolean): FiredEffect? {
    val actor = context.owner()?.let(::Actor) ?: triggerEvent.actor
    val hit = trigger.match(triggerEvent, actor, isSelf) ?: return null
    return FiredEffect(hit(instruction), actor, Cause(context, triggerEvent), automatic)
  }

  data class FiredEffect(
      val instruction: Instruction,
      val actor: Actor,
      val cause: Cause,
      val automatic: Boolean,
  ) {
    operator fun times(factor: Int) = copy(instruction = instruction * factor)
  }
}
