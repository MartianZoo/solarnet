package dev.martianzoo.engine

import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.engine.Engine.TaskListener
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.InstructionGroup
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEditedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.util.toSetStrict
import javax.inject.Inject

/**
 * ... With any change to the task queue, a set of normalizations is *always* applied. Here, the
 * notation `a >> b` is used for a task whose [Task.instruction] is `a` and whose [Task.then] is
 * `b`.
 * * Removing task `a >> b` first creates task `b >> null`
 * * `Ok >> b` is removed
 * * `Die >> b` or `a >> Die` produces [DeadEndException]
 * * `a, b >> null` is split into `a >> null` and `b >> null`
 * * `a, b >> c` produces some exception (which?)
 * * `a THEN b >> null` where `a THEN b` is separable is rewritten to `a >> b`
 * * `a THEN b >> c` where `a THEN b` is separable is rewritten to `a >> b THEN c`
 * * `a, Ok` becomes `a`
 * * `a, Die` becomes `Die`
 * * A concrete task with [Task.next] set is guaranteed to execute successfully
 * * New tasks created have the same owner and cause as the original. Prepared tasks cannot be split
 */
@GameScoped
internal class WritableTaskQueue @Inject constructor(private val events: TaskListener) : TaskQueue {
  private val taskSet: MutableSet<Task> = mutableSetOf()

  // OVERRIDES / READ-ONLY OPERATIONS

  override fun ids() = taskSet.toSetStrict { it.id }

  override fun contains(id: TaskId) = taskSet.any { it.id == id }

  override fun matching(predicate: (Task) -> Boolean) =
      taskSet.filter(predicate).toSetStrict { it.id }

  override fun <T> extract(extractor: (Task) -> T) = taskSet.map(extractor)

  override fun preparedTask(): TaskId? = taskSet.firstOrNull { it.next }?.id

  fun getTaskData(id: TaskId) =
      taskSet.firstOrNull { it.id == id } ?: error("nonexistent task: $id")

  private fun nextAvailableId() =
      if (taskSet.none()) TaskId("A") else taskSet.maxOf { it.id }.next()

  // ALL NON-PRIVATE MUTATIONS OF TASKSET

  internal fun addTasks(task: Task) = addTasks(split(task.instruction), task.owner, task.cause)

  internal fun addTasks(
      instruction: InstructionGroup,
      owner: Player,
      cause: Cause?,
  ): List<TaskAddedEvent> {
    val newTasks = Task.newTasks(nextAvailableId(), owner, instruction, cause)
    return newTasks.map {
      val task = addToTaskSet(it)
      events.taskAdded(task)
    }
  }

  internal fun removeTask(id: TaskId): TaskRemovedEvent {
    val task = getTaskData(id)
    removeFromTaskSet(task)
    return events.taskRemoved(task)
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
    val id = newTask.id
    val oldTask = getTaskData(id)
    if (newTask == oldTask) return null
    removeFromTaskSet(oldTask)
    addToTaskSet(newTask)
    return events.taskReplaced(oldTask, newTask)
  }

  // This method can get away without the normalizations/integrity-checks/whatever because it is
  // operating at a purely mechanical level, just undoing changes that were already made.
  // It's crucial that we ensure an entry got logged for every individual taskSet change.
  internal fun reverse(entry: TaskEvent) {
    when (entry) {
      is TaskAddedEvent -> removeFromTaskSet(entry.task)
      is TaskRemovedEvent -> addToTaskSet(entry.task)
      is TaskEditedEvent -> {
        removeFromTaskSet(entry.task)
        addToTaskSet(entry.oldTask)
      }
    }
  }

  // DIRECT MUTATORS

  private fun addToTaskSet(task: Task): Task {
    require(task.id != TaskId("ZZ"))
    require(taskSet.none { it.id == task.id })

    // What an amazing sorted set implementation
    val all: Set<Task> = taskSet + task
    taskSet.clear()
    taskSet += all.sortedBy { it.id }
    return task
  }

  private fun removeFromTaskSet(task: Task) {
    require(taskSet.remove(task))
  }

  override fun toString() = taskSet.joinToString("\n")
}
