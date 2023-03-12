package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent.Cause
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

  fun onChangeToSelf(triggerEvent: ChangeEvent, game: Game): FiredEffect? {
    val hit = trigger.matchSelf(triggerEvent, game) ?: return null
    return FiredEffect(hit.modify(instruction), hit.count, triggeredBy(triggerEvent), automatic)
  }

  fun onChangeToOther(triggerEvent: ChangeEvent, game: Game): FiredEffect? {
    val hit = trigger.matchOther(triggerEvent, game) ?: return null
    return FiredEffect(hit.modify(instruction), hit.count, triggeredBy(triggerEvent), automatic)
  }

  private fun triggeredBy(triggerEvent: ChangeEvent): Cause {
    val actor = context.owner()?.let(::Actor) ?: triggerEvent.cause?.actor ?: Actor.ENGINE
    return Cause(context, triggerEvent, actor)
  }

  data class FiredEffect(
      val instruction: Instruction,
      val multiplier: Int,
      val cause: Cause,
      val automatic: Boolean,
  ) {
    operator fun times(factor: Int) = copy(multiplier = multiplier * factor)

    fun scaledInstruction() = instruction * multiplier
  }
}
