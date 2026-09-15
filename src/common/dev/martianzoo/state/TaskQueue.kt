package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.state.Task.TaskId

/** A live read-only view of all pending tasks, optionally filtered to one [Actor]. */
public class TaskQueue
internal constructor(
    private val store: TaskStore,
    private val assignee: Actor?,
) {
  private fun filtered(): List<Task> =
      store.getAll().filter { assignee == null || it.assignee == assignee }

  private fun validateAssignee(task: Task) {
    if (assignee != null && task.assignee != assignee) {
      throw TaskException(
          "$assignee's queue can't contain a task assigned to ${task.assignee}: $task"
      )
    }
  }

  /** Task ids in stable diagnostic order. Their order has no gameplay meaning. */
  public fun ids(): Set<TaskId> = filtered().toSetStrict { it.id }

  public operator fun contains(id: TaskId): Boolean = filtered().any { it.id == id }

  public fun isEmpty(): Boolean = ids().none()

  public fun matching(predicate: (Task) -> Boolean): Set<TaskId> =
      filtered().filter(predicate).toSetStrict { it.id }

  public fun <T> extract(extractor: (Task) -> T): List<T> = filtered().map(extractor)

  public fun selectedTask(): TaskId? = filtered().firstOrNull { it.selected }?.id

  public fun getTaskData(id: TaskId): Task = store.get(id).also(::validateAssignee)

  override fun toString(): String = filtered().joinToString("\n")
}
