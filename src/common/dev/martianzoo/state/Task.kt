package dev.martianzoo.state

import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import kotlin.jvm.JvmInline

public data class Task(
    /** Identifies this task by the ordinal of its add event. Stable through task edits. */
    val id: TaskId,

    /** Who controls the surrounding operation and receives work caused by this task. */
    val controller: Actor,

    /**
     * Who receives this task when its controller selects it. That Actor supplies any remaining
     * choices and, absent a later `BY` handoff, executes the concrete instruction.
     */
    val selectionAssignee: Actor = controller,

    /** Who currently has exclusive authority to advance this task. */
    val assignee: Actor = controller,

    /** Whether this task has been selected and therefore holds the global select-lock. */
    val selected: Boolean = false,

    /** What to do. Can be abstract and is stored exactly as supplied by the engine. */
    val instruction: Instruction,

    /**
     * Independent work admitted immediately before this task's removal is recorded. Used for `THEN`
     * instructions. The continuation receives no priority and does not wait for queued consequences
     * of this task.
     */
    val then: InstructionGroup? = null,

    /** Why was this task born? */
    val cause: Cause?,
) {
  override fun toString(): String = buildString {
    append(id)
    append(if (selected) "* " else "  ")
    appendAssigneeLabel()
    append(instruction)
    then?.let { append(" (THEN $it)") }
    cause?.let { append(" $cause") }
  }

  private fun StringBuilder.appendAssigneeLabel() {
    append("[")
    append(assignee)
    append("] ")
  }

  /** A task's stable internal identity, wrapping the ordinal of its add event. */
  @JvmInline
  public value class TaskId(public val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }

    override fun toString(): String = ordinal.toString()
  }
}
