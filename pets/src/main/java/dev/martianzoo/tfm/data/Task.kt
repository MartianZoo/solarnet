package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.util.wrap

data class Task
constructor(
    /**
     * Identifies this task within a game at a particular point in time. These do get reused (for
     * user convenience) but of course no two have the same id at the same time.
     */
    val id: TaskId,

    /**
     * Might be abstract. If so, when actually performing the task a reification of the instruction
     * must be specified.
     */
    val instruction: Instruction,

    /** The player this task is waiting on -- think of there being N+1 separate queues. */
    val player: Player,

    /**
     * Why was this task born? If there's no reason, this *probably* never needed to be a Task at
     * all (but it can happen).
     */
    val cause: Cause?,

    /**
     * Why is the task still here? Often an error message. null probably means "because you weren't
     * in autoexec mode" I guess.
     */
    val whyPending: String? = null,
) {
  init {
    require(instruction !is Multi) { "should have been split up" }
  }

  override fun toString() = "$id: [$player] $instruction${whyPending.wrap(" (", ")")}"

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
