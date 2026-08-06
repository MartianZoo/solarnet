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

internal class WritableTaskQueue(
    private val taskQueues: TaskQueues,
    private val assignee: Actor?,
    private val predicate: (Task) -> Boolean,
) : TaskQueue {
  private fun filtered() = taskQueues.getAllTaskData().filter(predicate)

  private fun validateAssignee(task: Task) {
    if (assignee != null && task.assignee != assignee) {
      throw TaskException(
          "$assignee's queue can't contain a task assigned to ${task.assignee}: $task"
      )
    }
  }

  override fun ids() = filtered().toSetStrict { it.id }

  override fun contains(id: TaskId) = filtered().any { it.id == id }

  override fun areAllQueuesEmpty(): Boolean = taskQueues.getAllTaskData().none()

  override fun requireAllQueuesEmpty() {
    val allTasks = taskQueues.getAllTaskData()
    if (allTasks.any()) throw TaskException("pending tasks:\n${allTasks.joinToString("\n")}")
  }

  override fun matching(predicate: (Task) -> Boolean) =
      filtered().filter(predicate).toSetStrict { it.id }

  override fun <T> extract(extractor: (Task) -> T) = filtered().map(extractor)

  override fun preparedTask(): TaskId? = filtered().firstOrNull { it.next }?.id

  internal fun addTasks(
      instruction: InstructionGroup,
      cause: Cause?,
  ): List<TaskAddedEvent> {
    val inferredAssignee = assignee ?: error("global queue view can't infer a task assignee")
    return taskQueues.addTasks(instruction, inferredAssignee, cause)
  }

  internal fun addTasks(task: Task): List<TaskAddedEvent> {
    validateAssignee(task)
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

  internal fun queueFor(assignee: Actor): WritableTaskQueue = taskQueues[assignee]

  override fun toString() = filtered().joinToString("\n")
}
