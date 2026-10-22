package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.state.Task.TaskId

/**
 * A live read-only view of pending tasks, optionally filtered to an [Actor]. Each query observes
 * the current task store; returned collections are snapshots in task-creation order. This view does
 * not schedule execution or move a failed Admin task to the end of its retry order.
 */
public class TaskQueue
internal constructor(
    private val store: TaskStore,
    private val assignee: Actor?,
) {
  private fun filtered(): List<Task> =
      store.getAll().filter { assignee == null || it.assignee == assignee }

  private fun validateAssignee(task: Task) {
    if (assignee != null && task.assignee != assignee) {
      throw TaskException("`$assignee` cannot queue a task assigned to `${task.assignee}`: $task")
    }
  }

  /** Task ids in creation order. Priority and Admin retry order are handled by the caller. */
  public fun ids(): Set<TaskId> = filtered().toSetStrict { it.id }

  /** Whether task [id] is currently visible through this queue's assignee filter. */
  public operator fun contains(id: TaskId): Boolean = filtered().any { it.id == id }

  /** Whether this filtered queue currently contains no tasks. */
  public fun isEmpty(): Boolean = ids().none()

  /** Ids of currently visible tasks satisfying [predicate], in creation order. */
  public fun matching(predicate: (Task) -> Boolean): Set<TaskId> =
      filtered().filter(predicate).toSetStrict { it.id }

  /**
   * Applies [extractor] to each currently visible task in creation order and returns the results.
   */
  public fun <T> extract(extractor: (Task) -> T): List<T> = filtered().map(extractor)

  /** Id of the selected visible task, or null when none is selected. */
  public fun selectedTask(): TaskId? = filtered().firstOrNull { it.selected }?.id

  /**
   * Returns the current task with [id]. Fails if it does not exist or belongs to another assignee
   * than this queue's filter.
   */
  public fun getTaskData(id: TaskId): Task = store.get(id).also(::validateAssignee)

  override fun toString(): String = filtered().joinToString("\n")
}
