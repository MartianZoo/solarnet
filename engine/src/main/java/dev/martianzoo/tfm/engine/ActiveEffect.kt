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
    fun from(it: Effect, contextComponent: Component) =
        ActiveEffect(contextComponent, ActiveTrigger.from(it.trigger), it.automatic, it.instruction)
  }

  fun onChange(triggerEvent: ChangeEvent, game: Game, isSelf: Boolean): FiredEffect? {
    val actor = context.owner()?.let(::Actor) ?: triggerEvent.actor
    val hit = trigger.match(triggerEvent, actor, game, isSelf) ?: return null
    return FiredEffect(hit.modify(instruction), actor, triggeredBy(triggerEvent), automatic)
  }

  private fun triggeredBy(triggerEvent: ChangeEvent): Cause { // TODO check
    return Cause(context, triggerEvent)
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