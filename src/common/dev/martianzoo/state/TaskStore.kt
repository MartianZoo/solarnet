package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Task.TaskId

internal class TaskStore(
    private val backing: TaskStore? = null,
    private val requireValid: () -> Unit = {},
) {
  // A speculative store reads its backing until the first task event. A branch with no task
  // changes therefore costs no task-list copy.
  private var localTasks: MutableList<Task>? = if (backing == null) mutableListOf() else null

  private val tasks: MutableList<Task>
    get() {
      if (backing != null) requireValid()
      return localTasks ?: backing!!.getAll().toMutableList().also { localTasks = it }
    }

  internal fun fork(): TaskStore = TaskStore().also { copy -> copy.tasks += getAll() }

  internal fun overlay(requireValid: () -> Unit): TaskStore = TaskStore(this, requireValid)

  internal fun resetOverlay() {
    check(backing != null)
    requireValid()
    localTasks = null
  }

  internal fun all(): TaskQueue = TaskQueue(this, assignee = null)

  internal fun forAssignee(assignee: Actor): TaskQueue = TaskQueue(this, assignee)

  internal fun get(id: TaskId): Task =
      (localTasks?.also { if (backing != null) requireValid() } ?: backing?.getAll() ?: tasks)
          .firstOrNull { it.id == id } ?: throw TaskException("nonexistent task: $id")

  internal fun getAll(): List<Task> {
    if (backing != null) requireValid()
    return (localTasks ?: backing!!.getAll()).toList()
  }

  internal fun apply(entry: GameEvent.TaskEvent) {
    when (entry) {
      is GameEvent.TaskAddedEvent -> {
        require(tasks.none { it.id == entry.task.id })
        tasks += entry.task
      }
      is GameEvent.TaskRemovedEvent -> {
        require(get(entry.task.id) == entry.task)
        tasks.remove(entry.task)
      }
      is GameEvent.TaskEditedEvent -> {
        require(get(entry.task.id) == entry.oldTask)
        tasks[tasks.indexOf(entry.oldTask)] = entry.task
      }
    }
    tasks.sortBy { it.id.ordinal }
  }

  internal fun reverse(entry: GameEvent.TaskEvent) {
    when (entry) {
      is GameEvent.TaskAddedEvent -> {
        require(get(entry.task.id) == entry.task)
        tasks.remove(entry.task)
      }
      is GameEvent.TaskRemovedEvent -> {
        require(tasks.none { it.id == entry.task.id })
        tasks += entry.task
      }
      is GameEvent.TaskEditedEvent -> {
        require(get(entry.task.id) == entry.task)
        tasks[tasks.indexOf(entry.task)] = entry.oldTask
      }
    }
    tasks.sortBy { it.id.ordinal }
  }
}
