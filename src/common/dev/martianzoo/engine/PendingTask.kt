package dev.martianzoo.engine

import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.Player

/** Triggered work that has not yet been admitted to a task queue. */
internal data class PendingTask(
    val controller: Actor,
    val selectionAssignee: Actor = controller,
    val instruction: InstructionGroup,
    val cause: Cause,
) {
  operator fun times(factor: Int): PendingTask = copy(instruction = instruction * factor)

  internal companion object {
    /**
     * Routes effect work to the component's Player owner, then the changed component's Player
     * owner, then an explicitly supplied queued-effect fallback, then the triggering Actor. For
     * automatic work, an unowned effect uses the triggering Actor in place of both later choices.
     * The operation's Player controller retains the task until selection. Passive owners never gain
     * task authority.
     */
    fun fromEffect(
        context: Component,
        triggerEvent: ChangeEvent,
        controller: Actor,
        changedComponentPlayer: Player?,
        automatic: Boolean,
        instruction: InstructionGroup,
        queuedEffectFallback: Actor? = null,
    ): PendingTask {
      val effectPlayer = context.owningPlayer
      return PendingTask(
          controller =
              (controller as? Player)
                  ?: effectPlayer
                  ?: changedComponentPlayer
                  ?: triggerEvent.actor,
          selectionAssignee =
              effectPlayer
                  ?: changedComponentPlayer.takeUnless { automatic }
                  ?: queuedEffectFallback.takeUnless { automatic }
                  ?: triggerEvent.actor,
          instruction = instruction,
          cause = Cause(context.expression, triggerEvent.ordinal),
      )
    }
  }
}
