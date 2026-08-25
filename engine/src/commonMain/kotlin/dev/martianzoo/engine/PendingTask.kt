package dev.martianzoo.engine

import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause

/** Triggered work that has not yet been admitted to a task queue. */
// TODO: Contract this temporary tfm-tests seam.
public data class PendingTask(
    public val assignee: Actor,
    public val actor: Actor = assignee,
    public val instruction: InstructionGroup,
    public val cause: Cause,
) {
  public operator fun times(factor: Int): PendingTask = copy(instruction = instruction * factor)
}
