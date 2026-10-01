package dev.martianzoo.engine

import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause

/** Triggered work that has not yet been admitted to a task queue. */
internal data class PendingTask(
    val controller: Actor,
    val actor: Actor = controller,
    val instruction: InstructionGroup,
    val cause: Cause,
) {
  operator fun times(factor: Int): PendingTask = copy(instruction = instruction * factor)

  internal companion object {
    /**
     * Routes effect work to the component's Player owner, then the changed component's Player
     * owner, then the triggering Actor. For automatic work, an unowned effect uses the triggering
     * Actor in place of the changed component's owner. The operation's Player controller retains
     * the task until selection. Passive Owners never gain task authority.
     */
    fun fromEffect(
        context: Component,
        triggerEvent: ChangeEvent,
        controller: Actor,
        changedComponentPlayer: Player?,
        automatic: Boolean,
        instruction: InstructionGroup,
    ): PendingTask {
      val effectOwner = context.playerOwner
      return PendingTask(
          controller =
              (controller as? Player)
                  ?: effectOwner
                  ?: changedComponentPlayer
                  ?: triggerEvent.actor,
          actor =
              effectOwner ?: changedComponentPlayer.takeUnless { automatic } ?: triggerEvent.actor,
          instruction = instruction,
          cause = Cause(context.expression, triggerEvent.ordinal),
      )
    }
  }
}
