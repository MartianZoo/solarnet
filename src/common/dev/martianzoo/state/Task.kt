package dev.martianzoo.state

import dev.martianzoo.pets.Transforming
import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import kotlin.jvm.JvmInline

/**
 * Current or recorded form of a pending instruction. [id] survives edits. [assignee] controls the
 * task. [selected] holds the world's exclusive selection lock until the task is narrowed, executed,
 * or removed. [instruction] may still require choices or dispatch separate work through `TASK` or
 * `LATER`. A [deferred] task waits until no earlier-deferred work remains.
 */
public data class Task(
    /** Identifies this task by the ordinal of its add event. Stable through task edits. */
    val id: TaskId,

    /** Who currently has exclusive authority to advance this task. */
    val assignee: Actor,

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

    /**
     * Whether `LATER` or `FINALLY` admitted this task ([rules L2-18 and
     * L2-19](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md#2-instructions)).
     * No task may be selected while a task with an earlier [Deferral] remains. When no ordinary
     * task remains, every task with the earliest remaining deferral becomes ordinary at once.
     */
    val deferral: Deferral = Deferral.NONE,
) {
  init {
    require(!deferred || !selected) { "a deferred task cannot be selected: $this" }
  }

  /** Whether this task waits for ordinary, or earlier-deferred, work to finish. */
  val deferred: Boolean
    get() = deferral != Deferral.NONE

  /**
   * Returns a proposed version of this task with its current instruction's linked X set to [value].
   */
  public fun bindXTo(value: Int): Task =
      copy(instruction = Transforming.bindXTo(value).transformInstruction(instruction))

  override fun toString(): String = buildString {
    append(id)
    append(if (selected) "* " else "  ")
    appendAssigneeLabel()
    if (deferred) append("(${deferral.keyword}) ")
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
