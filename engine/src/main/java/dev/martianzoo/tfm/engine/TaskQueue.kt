package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId

interface TaskQueue {
  val size: Int
  val ids: Set<TaskId>

  operator fun contains(id: TaskId): Boolean
  operator fun get(id: TaskId): Task

  fun isEmpty(): Boolean
  fun nextAvailableId(): TaskId
  fun toStrings(): List<String>
}
