package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.Engine.TaskListener
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.InstructionGroup
import dev.martianzoo.util.toSetStrict

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
 * * New tasks created have the same actor and cause as the original. Prepared tasks cannot be split
 */
internal class TaskQueues(private val events: TaskListener) {
  private val taskSet: MutableSet<Task> = mutableSetOf()

  internal fun all(): WritableTaskQueue = WritableTaskQueue(this, actor = null) { true }

  internal operator fun get(actor: Actor): WritableTaskQueue =
      WritableTaskQueue(this, actor = actor) { it.actor == actor }

  // READ-ONLY OPERATIONS NEEDED BY MUTATORS

  internal operator fun contains(id: TaskId) = taskSet.any { it.id == id }

  internal fun preparedTask(): TaskId? = taskSet.firstOrNull { it.next }?.id

  internal fun getTaskData(id: TaskId) =
      taskSet.firstOrNull { it.id == id } ?: error("nonexistent task: $id")

  internal fun getAllTaskData(): List<Task> = taskSet.toList()

  private fun nextAvailableId() =
      if (taskSet.none()) TaskId("A") else taskSet.maxOf { it.id }.next()

  // ALL NON-PRIVATE MUTATIONS OF TASKSET

  internal fun addTasks(task: Task) = addTasks(split(task.instruction), task.actor, task.cause)

  internal fun addTasks(
      instruction: InstructionGroup,
      actor: Actor,
      cause: Cause?,
  ): List<TaskAddedEvent> {
    val newTasks = Task.newTasks(nextAvailableId(), actor, instruction, cause)
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

internal class WritableTaskQueue(
    private val taskQueues: TaskQueues,
    private val actor: Actor?,
    private val predicate: (Task) -> Boolean,
) : TaskQueue {
  private fun filtered() = taskQueues.getAllTaskData().filter(predicate)

  private fun validateActor(task: Task) {
    if (actor != null && task.actor != actor) {
      error("$actor can't act on a task assigned to ${task.actor}: $task")
    }
  }

  override fun ids() = filtered().toSetStrict { it.id }

  override fun contains(id: TaskId) = filtered().any { it.id == id }

  override fun areAllQueuesEmpty(): Boolean = taskQueues.getAllTaskData().none()

  override fun requireAllQueuesEmpty() {
    val allTasks = taskQueues.getAllTaskData()
    require(allTasks.none()) { allTasks.joinToString("\n") }
  }

  override fun matching(predicate: (Task) -> Boolean) =
      filtered().filter(predicate).toSetStrict { it.id }

  override fun <T> extract(extractor: (Task) -> T) = filtered().map(extractor)

  override fun preparedTask(): TaskId? = filtered().firstOrNull { it.next }?.id

  fun addTasks(
      instruction: InstructionGroup,
      cause: Cause?,
  ): List<TaskAddedEvent> {
    val taskActor = actor ?: error("global queue view can't infer an actor for new tasks")
    return taskQueues.addTasks(instruction, taskActor, cause)
  }

  fun addTasks(task: Task): List<TaskAddedEvent> {
    validateActor(task)
    return taskQueues.addTasks(task)
  }

  fun removeTask(id: TaskId): TaskRemovedEvent {
    validateActor(getTaskData(id))
    return taskQueues.removeTask(id)
  }

  fun editTask(newTask: Task): TaskEditedEvent? {
    validateActor(newTask)
    validateActor(getTaskData(newTask.id))
    return taskQueues.editTask(newTask)
  }

  fun getTaskData(id: TaskId): Task = taskQueues.getTaskData(id).also(::validateActor)

  fun queueFor(actor: Actor): WritableTaskQueue = taskQueues[actor]

  override fun toString() = filtered().joinToString("\n")
}
