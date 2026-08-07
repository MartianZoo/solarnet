package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.InstructionGroup
import dev.martianzoo.types.ClassTable

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
    private val events: EventLog,
    private val classTable: ClassTable?,
    initialTasks: Collection<Task>,
) {
  init {
    require(initialTasks.all { it.id.ordinal < events.size })
  }

  internal constructor(
      events: EventLog,
      classTable: ClassTable? = null,
  ) : this(events, classTable, emptyList())

  private val taskSet: MutableSet<Task> = initialTasks.toMutableSet()
  private val isAbstract: ((Expression) -> Boolean)? = classTable?.let { table ->
    { expression -> table.resolve(expression).abstract }
  }

  /** Copies current tasks without recording their existing additions in [events]. */
  internal fun copy(events: EventLog) = TaskQueues(events, classTable, taskSet)

  internal fun all(): TaskQueue = TaskQueue(this, assignee = null) { true }

  internal operator fun get(assignee: Actor): TaskQueue =
      TaskQueue(this, assignee = assignee) { it.assignee == assignee }

  // READ-ONLY OPERATIONS NEEDED BY MUTATORS

  internal fun getTaskData(id: TaskId) =
      taskSet.firstOrNull { it.id == id } ?: throw TaskException("nonexistent task: $id")

  internal fun getAllTaskData(): List<Task> = taskSet.toList()

  // ALL NON-PRIVATE MUTATIONS OF TASKSET

  internal fun addTasks(task: PendingTask) =
      addTasks(split(task.instruction), task.assignee, task.cause)

  internal fun addTasks(
      instruction: InstructionGroup,
      assignee: Actor,
      cause: Cause?,
  ): List<TaskAddedEvent> {
    val newTasks = Task.newTasks(TaskId(events.size), assignee, instruction, cause, isAbstract)
    return newTasks.map {
      require(it.id.ordinal == events.size)
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
