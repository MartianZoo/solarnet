package dev.martianzoo.engine

import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause

/** Triggered work that has not yet been admitted to a task queue. */
internal data class PendingTask(
    val assignee: Actor,
    val actor: Actor = assignee,
    val instruction: InstructionGroup,
    val cause: Cause,
) {
  operator fun times(factor: Int): PendingTask = copy(instruction = instruction * factor)
}
