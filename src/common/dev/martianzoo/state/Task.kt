package dev.martianzoo.state

import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import kotlin.jvm.JvmInline

public data class Task(
    /** Identifies this task by the ordinal of its add event. Stable through task edits. */
    val id: TaskId,

    /** Who controls the surrounding operation and receives work caused by this task. */
    val controller: Actor,

    /**
     * Who supplies choices after selection and performs resulting changes unless the instruction
     * contains an explicit BY.
     */
    val actor: Actor = controller,

    /** Where this task is in its selection lifecycle. */
    val selection: Selection = Selection.UNSELECTED,

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

  /** Whose pending-work queue contains this task and whose scoped Agent may act on it. */
  public val assignee: Actor
    get() = if (selection == Selection.DELEGATED) actor else controller

  /** If true, the world may not be modified until this task is completed. */
  public val selected: Boolean
    get() = selection != Selection.UNSELECTED

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

  /** The selection states that determine a task's assignee and whether it locks the World. */
  public enum class Selection {
    UNSELECTED,
    SELECTED,
    DELEGATED,
  }
}
