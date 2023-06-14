package dev.martianzoo.engine

import dev.martianzoo.tfm.data.Task

/**
 * Contains tasks: what the game is waiting on someone to do. Each task is owned by some [Player]
 * (which could be the engine itself). Normally, a state should never been observed in which engine
 * tasks remain, as the engine should always be able to take care of them itself before returning.
 *
 * It is possible to retrieve the [Task] corresponding to a [TaskId] but this is generally
 * discouraged and the API doesn't make it easy.
 */
public interface TaskQueue {
  /** Returns the id of each task currently in the queue, in order from oldest to newest. */
  fun ids(): Set<Task.TaskId>

  operator fun contains(id: Task.TaskId): Boolean

  /** Returns true if the queue is empty. */
  fun isEmpty() = ids().none()

  /** Returns all task ids whose task data matches the given predicate. */
  fun matching(predicate: (Task) -> Boolean): Set<Task.TaskId>

  /** Returns the results of executing a function against every task in the queue. */
  fun <T> extract(extractor: (Task) -> T): List<T>

  /** Returns the id of the task marked with [Task.next] if there is one. */
  fun preparedTask(): Task.TaskId?
}
