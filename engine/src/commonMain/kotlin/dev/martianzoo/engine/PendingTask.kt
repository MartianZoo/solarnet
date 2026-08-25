package dev.martianzoo.engine

import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.ast.InstructionGroup

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
