package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId

/**
 * Contains tasks: what the game engine is waiting on someone to do. Each task is owned by some
 * [Actor] (a player or the engine). Normally, a state should never been observed in which engine
 * tasks remain, as the engine should always be able to take care of them itself before returning.
 *
 * This interface speaks entirely in terms of [TaskId]s.
 */
interface TaskQueue {
  val size: Int
  val ids: Set<TaskId>

  operator fun contains(id: TaskId): Boolean
  operator fun get(id: TaskId): Task

  fun isEmpty(): Boolean
  fun nextAvailableId(): TaskId
  fun toStrings(): List<String>
  fun asMap(): Map<TaskId, Task>
}
