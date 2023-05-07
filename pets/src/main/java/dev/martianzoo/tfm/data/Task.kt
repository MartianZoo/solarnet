package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Transform

public data class Task(
    /**
     * Identifies this task within a game at a particular point in time. These do get reused (for
     * user convenience) but of course no two have the same id at the same time.
     */
    val id: TaskId,

    /**
     * The player (or engine) this task is waiting on, who has the right to narrow it as they wish,
     * and execute it.
     */
    val owner: Player,

    /** If true, no game state may be modified until this task is completed. */
    val next: Boolean = false,

    /**
     * What to do. Might be abstract. If so, it will have to be narrowed to something concrete
     * before/when it is executed.
     */
    val instruction: Instruction,

    /**
     * Any instruction that should be automatically enqueued when this task is handled successfully.
     * Used for `THEN` instructions.
     */
    val then: Instruction? = null,

    /** Why was this task born? */
    val cause: Cause?,

    /** Why is the task still here? Often an error message. */
    val whyPending: String? = null,
) {
  init {
    when (instruction) {
      is Change -> require(instruction.gaining != DIE.expression)
      is Multi, is NoOp, is Transform -> error("can't enqueue: $instruction")
      else -> {}
    }
  }

  override fun toString() = buildString {
    append(id)
    append(if (next) "* " else "  ")
    append("[$owner] ")
    append(instruction)
    then?.let { append(" (THEN $it)") }
    whyPending?.let { append(" ($it) ") }
  }

  data class TaskId(val s: String) : Comparable<TaskId> {
    init {
      require(s.length in 1..2) { s }
      require(s.all { it in 'A'..'Z' })
    }

    override fun compareTo(other: TaskId) = s.padStart(2).compareTo(other.s.padStart(2))

    override fun toString() = s

    fun next(): TaskId {
      val news =
          when {
            s == "Z" -> "AA"
            s.length == 1 -> "${s[0] + 1}"
            s[1] == 'Z' -> "${s[0] + 1}A"
            else -> "${s[0]}${s[1] + 1}"
          }
      return TaskId(news)
    }
  }
}
