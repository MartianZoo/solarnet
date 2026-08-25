package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.TaskAddedEvent
import dev.martianzoo.pets.data.GameEvent.TaskEditedEvent
import dev.martianzoo.pets.data.GameEvent.TaskEvent
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.types.ClassTable

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
 * * New tasks created have the same assignee, Actor, and cause as the original. Prepared tasks
 *   cannot be split
 */
// TODO: Contract this temporary tfm-tests seam.
public class TaskQueues
private constructor(
    private val events: EventLog,
    private val classTable: ClassTable?,
    initialTasks: Collection<Task>,
) {
  init {
    require(initialTasks.all { it.id.ordinal < events.size })
  }

  public constructor(
      events: EventLog,
      classTable: ClassTable? = null,
  ) : this(events, classTable, emptyList())

  private val taskSet: MutableSet<Task> = initialTasks.toMutableSet()
  private val isAbstract: ((Expression) -> Boolean)? = classTable?.let { table ->
    { expression -> table.resolve(expression).abstract }
  }

  /** Copies current tasks without recording their existing additions in [events]. */
  public fun copy(events: EventLog): TaskQueues = TaskQueues(events, classTable, taskSet)

  internal fun all(): TaskQueue = TaskQueue(this, assignee = null) { true }

  public operator fun get(assignee: Actor): TaskQueue =
      TaskQueue(this, assignee = assignee) { it.assignee == assignee }

  // READ-ONLY OPERATIONS NEEDED BY MUTATORS

  internal fun getTaskData(id: TaskId) =
      taskSet.firstOrNull { it.id == id } ?: throw TaskException("nonexistent task: $id")

  internal fun getAllTaskData(): List<Task> = taskSet.toList()

  // ALL NON-PRIVATE MUTATIONS OF TASKSET

  internal fun addTasks(task: PendingTask) =
      addTasks(task.instruction, task.assignee, task.cause, task.actor)

  internal fun addTasks(
      instruction: InstructionGroup,
      assignee: Actor,
      cause: Cause?,
      actor: Actor = assignee,
  ): List<TaskAddedEvent> {
    val newTasks =
        Task.newTasks(TaskId(events.size), assignee, instruction, cause, actor, isAbstract)
    return newTasks.map {
      require(it.id.ordinal == events.size)
      apply(TaskAddedEvent(events.nextOrdinal, it))
    }
  }

  internal fun removeTask(id: TaskId): TaskRemovedEvent {
    val task = getTaskData(id)
    return apply(TaskRemovedEvent(events.nextOrdinal, task))
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
    val id = newTask.id
    val oldTask = getTaskData(id)
    if (newTask == oldTask) return null
    return apply(TaskEditedEvent(events.nextOrdinal, oldTask = oldTask, task = newTask))
  }

  /** Applies and records one task event. This is also the task-history replay boundary. */
  private fun <E : TaskEvent> apply(entry: E): E =
      events.record(entry) {
        when (entry) {
          is TaskAddedEvent -> addToTaskSet(entry.task)
          is TaskRemovedEvent -> removeFromTaskSet(entry.task)
          is TaskEditedEvent -> {
            removeFromTaskSet(entry.oldTask)
            addToTaskSet(entry.task)
          }
        }
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

  private fun addToTaskSet(task: Task) {
    require(taskSet.none { it.id == task.id })

    // Task ids define a stable diagnostic order, though queue order has no gameplay meaning.
    val all: Set<Task> = taskSet + task
    taskSet.clear()
    taskSet += all.sortedBy { it.id }
  }

  private fun removeFromTaskSet(task: Task) {
    require(taskSet.remove(task))
  }

  override fun toString(): String = taskSet.joinToString("\n")
}
