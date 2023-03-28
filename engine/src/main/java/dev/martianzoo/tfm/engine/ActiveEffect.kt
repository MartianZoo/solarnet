package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction

data class ActiveEffect(
    val original: Effect,
    val context: Component,
    val trigger: ActiveTrigger,
    val automatic: Boolean,
    val instruction: Instruction,
) {
  companion object {
    fun from(it: Effect, context: Component, game: Game, triggerLinkages: Set<ClassName>) =
        ActiveEffect(
            it,
            context,
            ActiveTrigger.from(it.trigger, context, game, triggerLinkages),
            it.automatic,
            it.instruction)
  }

  operator fun times(multiplier: Int) = copy(instruction = instruction * multiplier)

  fun onChangeToSelf(triggerEvent: ChangeEvent) = onChange(triggerEvent, isSelf = true)

  fun onChangeToOther(triggerEvent: ChangeEvent) = onChange(triggerEvent, isSelf = false)

  private fun onChange(triggerEvent: ChangeEvent, isSelf: Boolean): FiredEffect? {
    val player = context.owner()?.let(::Player) ?: triggerEvent.player
    val hit = trigger.match(triggerEvent, player, isSelf) ?: return null
    return FiredEffect(hit(instruction), player, Cause(context, triggerEvent), automatic)
  }

  data class FiredEffect(
      val instruction: Instruction,
      val player: Player,
      val cause: Cause,
      val automatic: Boolean,
  ) {
    operator fun times(factor: Int) = copy(instruction = instruction * factor)
  }
}
