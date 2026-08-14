package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.pets.ast.Instruction.InstructionGroup
import dev.martianzoo.util.toSetStrict

/**
 * Contains tasks: what the game is waiting on someone to do. Each task has an assignee, currently
 * represented by an [Actor]. Normally, a state should never be observed in which administrative
 * tasks remain, as the engine should always be able to take care of them itself before returning.
 *
 * It is possible to retrieve the [Task] corresponding to a task id but this is generally
 * discouraged and the API doesn't make it easy.
 */
public class TaskQueue
internal constructor(
    private val taskQueues: TaskQueues,
    private val assignee: Actor?,
    private val predicate: (Task) -> Boolean,
) {
  private fun filtered(): List<Task> = taskQueues.getAllTaskData().filter(predicate)

  private fun validateAssignee(task: Task) {
    if (assignee != null && task.assignee != assignee) {
      throw TaskException(
          "$assignee's queue can't contain a task assigned to ${task.assignee}: $task"
      )
    }
  }

  /**
   * Returns the id of each task currently in the queue. Current iteration order is stable by id so
   * arbitrary choices are reproducible, but has no gameplay meaning.
   */
  public fun ids(): Set<TaskId> = filtered().toSetStrict { it.id }

  public operator fun contains(id: TaskId): Boolean = filtered().any { it.id == id }

  /** Returns true if the queue is empty. */
  public fun isEmpty(): Boolean = ids().none()

  /** Returns all task ids whose task data matches the given predicate. */
  public fun matching(predicate: (Task) -> Boolean): Set<TaskId> =
      filtered().filter(predicate).toSetStrict { it.id }

  /** Returns the results of executing a function against every task in the queue. */
  public fun <T> extract(extractor: (Task) -> T): List<T> = filtered().map(extractor)

  /** Returns the id of the task marked with [Task.next] if there is one. */
  public fun preparedTask(): TaskId? = filtered().firstOrNull { it.next }?.id

  /** Returns true if no queue has any tasks. */
  internal fun areAllQueuesEmpty(): Boolean = taskQueues.getAllTaskData().none()

  /** Throws if any queue has any tasks. */
  internal fun requireAllQueuesEmpty() {
    val allTasks = taskQueues.getAllTaskData()
    if (allTasks.any()) throw TaskException("pending tasks:\n${allTasks.joinToString("\n")}")
  }

  internal fun addTasks(
      instruction: InstructionGroup,
      cause: Cause?,
      actor: Actor? = null,
  ): List<TaskAddedEvent> {
    val inferredAssignee = assignee ?: error("global queue view can't infer a task assignee")
    return taskQueues.addTasks(instruction, inferredAssignee, cause, actor ?: inferredAssignee)
  }

  internal fun addTasks(task: PendingTask): List<TaskAddedEvent> {
    if (assignee != null && task.assignee != assignee) {
      throw TaskException(
          "$assignee's queue can't contain pending work assigned to ${task.assignee}: $task"
      )
    }
    return taskQueues.addTasks(task)
  }

  internal fun removeTask(id: TaskId): TaskRemovedEvent {
    validateAssignee(getTaskData(id))
    return taskQueues.removeTask(id)
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
    validateAssignee(newTask)
    validateAssignee(getTaskData(newTask.id))
    return taskQueues.editTask(newTask)
  }

  internal fun getTaskData(id: TaskId): Task = taskQueues.getTaskData(id).also(::validateAssignee)

  internal fun queueFor(assignee: Actor): TaskQueue = taskQueues[assignee]

  override fun toString(): String = filtered().joinToString("\n")
}
