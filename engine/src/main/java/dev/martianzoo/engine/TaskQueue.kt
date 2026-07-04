package dev.martianzoo.engine

import dev.martianzoo.data.Player
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.pets.ast.Instruction.InstructionGroup

/**
 * Contains tasks: what the game is waiting on someone to do. Each task is owned by some [Player]
 * (which could be the engine itself). Normally, a state should never be observed in which engine
 * tasks remain, as the engine should always be able to take care of them itself before returning.
 *
 * It is possible to retrieve the [Task] corresponding to a [TaskId] but this is generally
 * discouraged and the API doesn't make it easy.
 */
public interface TaskQueue {
  /** Returns the id of each task currently in the queue, in order from oldest to newest. */
  fun ids(): Set<TaskId>

  operator fun contains(id: TaskId): Boolean

  /** Returns true if the queue is empty. */
  fun isEmpty() = ids().none()

  /** Returns all task ids whose task data matches the given predicate. */
  fun matching(predicate: (Task) -> Boolean): Set<TaskId>

  /** Returns the results of executing a function against every task in the queue. */
  fun <T> extract(extractor: (Task) -> T): List<T>

  /** Returns the id of the task marked with [Task.next] if there is one. */
  fun preparedTask(): TaskId?

  /** Returns true if no queue has any tasks. */
  fun areAllQueuesEmpty(): Boolean

  /** Throws if any queue has any tasks. */
  fun requireAllQueuesEmpty()
}

internal interface WritableTaskQueue : TaskQueue {
  fun addTasks(instruction: InstructionGroup, cause: Cause?): List<TaskAddedEvent>

  fun addTasks(task: Task): List<TaskAddedEvent>

  fun removeTask(id: TaskId): TaskRemovedEvent

  fun editTask(newTask: Task): TaskEditedEvent?

  fun getTaskData(id: TaskId): Task

  /** Returns the id of each task in any queue, in order from oldest to newest. */
  fun idsInAllQueues(): Set<TaskId>

  /** Returns true if any queue contains [id]. */
  fun containsInAnyQueue(id: TaskId): Boolean

  /** Returns the id of the task marked with [Task.next] in any queue if there is one. */
  fun preparedTaskInAnyQueue(): TaskId?

  fun getTaskDataInAnyQueue(id: TaskId): Task

  fun queueFor(player: Player): WritableTaskQueue
}
