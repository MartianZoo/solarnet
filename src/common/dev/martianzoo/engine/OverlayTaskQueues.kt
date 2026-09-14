package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.types.ClassTable

/** Task reads from one stable queue amended by this overlay's task-event tail. */
internal class OverlayTaskQueues(
    events: EventLog,
    classTable: ClassTable?,
    private val backing: TaskQueues,
    private val requireUnchangedBacking: () -> Unit,
) : TaskQueues(events, classTable) {
  private var materializedTasks: MutableList<Task>? = null

  override fun getTaskData(id: TaskId): Task =
      findTaskData(id) ?: throw TaskException("nonexistent task: $id")

  override fun getAllTaskData(): List<Task> {
    requireUnchangedBacking()
    return materialize().toList()
  }

  override fun addToTaskSet(task: Task) {
    require(findTaskData(task.id) == null)
    val tasks = materialize()
    tasks += task
    tasks.sortBy { it.id.ordinal }
  }

  override fun removeFromTaskSet(task: Task) {
    require(findTaskData(task.id) == task)
    materialize().remove(task)
  }

  override fun toString(): String = getAllTaskData().joinToString("\n")

  private fun materialize(): MutableList<Task> =
      materializedTasks ?: backing.getAllTaskData().toMutableList().also { materializedTasks = it }

  private fun findTaskData(id: TaskId): Task? {
    requireUnchangedBacking()
    val tasks = materializedTasks
    return if (tasks == null) backing.getAllTaskData().firstOrNull { it.id == id }
    else tasks.firstOrNull { it.id == id }
  }
}
