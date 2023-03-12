package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.LogEntry.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.util.wrap

data class Task(
    val id: TaskId,
    val instruction: Instruction,
    val owner: Actor,
    val cause: Cause? = null,
    val whyPending: String? = null,
) {
  init {
    require(instruction !is Multi) { "should have been split up" }
  }

  override fun toString() = "$id: [$owner] $instruction${whyPending.wrap(" (", ")")}"

  data class TaskId(val s: String) : Comparable<TaskId> {
    init {
      require(s.length in 1..2)
      require(s.all { it in 'A'..'Z' })
    }

    override fun compareTo(other: TaskId) = s.padStart(2).compareTo(other.s.padStart(2))

    override fun toString() = s

    fun next(): TaskId {
      val news = when {
        s == "Z" -> "AA"
        s.length == 1 -> "${s[0] + 1}"
        s[1] == 'Z' -> "${s[0] + 1}A"
        else -> "${s[0]}${s[1] + 1}"
      }
      return TaskId(news)
    }
  }
}
