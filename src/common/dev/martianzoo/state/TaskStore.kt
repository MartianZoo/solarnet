package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.state.Task.TaskId

internal class TaskStore {
  private val tasks = mutableListOf<Task>()

  internal fun all(): TaskQueue = TaskQueue(this, assignee = null)

  internal fun forAssignee(assignee: Actor): TaskQueue = TaskQueue(this, assignee)

  internal fun get(id: TaskId): Task =
      tasks.firstOrNull { it.id == id } ?: throw TaskException("nonexistent task: $id")

  internal fun getAll(): List<Task> = tasks.toList()

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
