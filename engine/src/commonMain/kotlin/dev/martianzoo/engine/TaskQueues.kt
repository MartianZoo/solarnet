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
 * With any change to the task queue, a set of normalizations is *always* applied. Here, the
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
 * * `a OR Die` becomes `a`; if every option is `Die`, the task produces [DeadEndException]
 * * A concrete task with [Task.next] set is guaranteed to execute successfully
 * * New tasks created have the same assignee and cause as the original. Prepared tasks cannot be
 *   split
 */
internal class TaskQueues
private constructor(
    private val events: TaskListener,
    initialTasks: Collection<Task>,
) {
  internal constructor(events: TaskListener) : this(events, emptyList())

  private val taskSet: MutableSet<Task> = initialTasks.toMutableSet()

  /** Copies current tasks without recording their existing additions in [events]. */
  internal fun copy(events: TaskListener) = TaskQueues(events, taskSet)

  internal fun all(): WritableTaskQueue = WritableTaskQueue(this, assignee = null) { true }

  internal operator fun get(assignee: Actor): WritableTaskQueue =
      WritableTaskQueue(this, assignee = assignee) { it.assignee == assignee }

  // READ-ONLY OPERATIONS NEEDED BY MUTATORS

  internal fun getTaskData(id: TaskId) =
      taskSet.firstOrNull { it.id == id } ?: error("nonexistent task: $id")

  internal fun getAllTaskData(): List<Task> = taskSet.toList()

  private fun nextAvailableId() =
      if (taskSet.none()) TaskId("A") else taskSet.maxOf { it.id }.next()

  // ALL NON-PRIVATE MUTATIONS OF TASKSET

  internal fun addTasks(task: Task) = addTasks(split(task.instruction), task.assignee, task.cause)

  internal fun addTasks(
      instruction: InstructionGroup,
      assignee: Actor,
      cause: Cause?,
  ): List<TaskAddedEvent> {
    val newTasks = Task.newTasks(nextAvailableId(), assignee, instruction, cause)
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
    private val assignee: Actor?,
    private val predicate: (Task) -> Boolean,
) : TaskQueue {
  private fun filtered() = taskQueues.getAllTaskData().filter(predicate)

  private fun validateAssignee(task: Task) {
    if (assignee != null && task.assignee != assignee) {
      error("$assignee's queue can't contain a task assigned to ${task.assignee}: $task")
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
