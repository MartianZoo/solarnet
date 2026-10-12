package dev.martianzoo.engine

import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.Player

/** Triggered or dispatched work that has not yet been admitted to a task queue. */
internal data class PendingTask(
    val assignee: Actor,
    val instruction: InstructionGroup,
    val cause: Cause?,
    /** The deferral of the tasks this work becomes; see [dev.martianzoo.state.Task.deferral]. */
    val deferral: Deferral = Deferral.NONE,
) {
  operator fun times(factor: Int): PendingTask = copy(instruction = instruction * factor)

  internal companion object {
    /**
     * Routes effect work to its Player owner, then the changed component's Player owner, then the
     * Actor that made the change. Passive owners never gain task authority.
     */
    fun fromEffect(
        context: Component,
        triggerEvent: ChangeEvent,
        changedComponentPlayer: Player?,
        automatic: Boolean,
        instruction: InstructionGroup,
    ): PendingTask {
      val effectPlayer = context.owningPlayer
      return PendingTask(
          assignee =
              effectPlayer ?: changedComponentPlayer.takeUnless { automatic } ?: triggerEvent.actor,
          instruction = instruction,
          cause = Cause(context.expression, triggerEvent.ordinal),
      )
    }
  }
}
