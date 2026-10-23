package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.Transaction

/**
 * Constructs and edits task data using engine normalization before recording exact state events.
 * Here, `a >> b` is a task whose [Task.instruction] is `a` and whose [Task.then] is `b`.
 * * Removing task `a >> b` first creates task `b >> null`
 * * `Ok >> b` is removed
 * * Mandatory `Die >> b` or `a >> Die` produces [DeadEndException]; nonmandatory `Die` is `Ok`
 * * `a, b >> null` is split into `a >> null` and `b >> null`
 * * A Task cannot store a group as its instruction. If resolution exposes independent children,
 *   ActorEngine admits those children and the original continuation as ordinary pending work
 * * `a THEN b >> null` where `a THEN b` is separable is rewritten to `a >> b`
 * * `a THEN b >> c` keeps the two independent variable scopes separate
 * * `a, Ok` becomes `a`
 * * `a, Die!` becomes `Die!`
 * * `a OR Die!` becomes `a`; if every option is mandatory `Die`, the task produces
 *   [DeadEndException]
 * * A concrete selected task has no remaining choices, but its effects or invariant checks can
 *   still fail. Normalization does not establish transaction legality
 * * Normalization retains task identity, current assignee, selection, and cause. Admission and
 *   contextual selection assign concrete System gains and removals to Admin, including changes
 *   scaled by `Per` or waiting behind a gate. Concrete gains explicitly `BY Admin` also begin with
 *   Admin. System assignment dispatches a continuation back to the former assignee; explicit `BY`
 *   retains its own continuation scope. Abstract changes remain for the current assignee to choose.
 *   Selected tasks cannot be replaced by independent siblings
 * * Once a completed transaction leaves no ordinary task, every task with the earliest remaining
 *   [Task.deferral] becomes ordinary at once
 * * An ordinary task whose instruction is a dispatch is performed at once: `TASK R { b } >> c`
 *   becomes task `b` for R, with the dispatch's deferral, and task `c`
 */
internal class TaskQueues(
    private val gameWorld: GameWorld,
    private val transactionForEvent: () -> Transaction? = { null },
    private val requireAtEnd: () -> Unit = {},
) {
  private val classTable: ClassTable = gameWorld.classTable
  private val systemClass = classTable.getClass(SYSTEM)
  private val isAbstract: (Expression) -> Boolean = { expression ->
    classTable.resolve(expression).abstract
  }

  internal fun addTasks(task: PendingTask) =
      addTasks(task.instruction, task.assignee, task.cause, task.deferral)

  internal fun addTasks(
      instruction: InstructionGroup,
      assignee: Actor,
      cause: Cause?,
      deferral: Deferral = Deferral.NONE,
  ): List<TaskAddedEvent> {
    requireAtEnd()
    return admit(instruction, assignee, cause, deferral)
  }

  /**
   * Adds [instruction]'s tasks without promoting deferred work, so that a whole group is admitted
   * before promotion is considered. An ordinary task that is only a dispatch is never added: the
   * dispatch is performed instead.
   */
  private fun admit(
      instruction: InstructionGroup,
      assignee: Actor,
      cause: Cause?,
      deferral: Deferral,
  ): List<TaskAddedEvent> =
      taskInstructions(instruction).flatMap {
        val task =
            assignConcreteChangeToAdmin(
                newTask(TaskId(gameWorld.nextOrdinal), assignee, it, cause, isAbstract, deferral)
            )
        val dispatch = task.instruction as? Instruction.Dispatch
        if (dispatch != null && !task.deferred) {
          perform(dispatch, task)
        } else {
          listOf(
              gameWorld.apply(TaskAddedEvent(gameWorld.nextOrdinal, task, transactionForEvent()))
          )
        }
      }

  /**
   * Performs [dispatch], the instruction of ordinary [task]: its body becomes work for its
   * recipient with its deferral (L2-17 to L2-19), and any continuation of [task] follows at once,
   * as it would after any finished task. Nothing about a dispatch is left to decide.
   */
  private fun perform(dispatch: Instruction.Dispatch, task: Task): List<TaskAddedEvent> =
      admit(
          InstructionGroup.of(dispatch.body),
          gameWorld.reader.recipientOf(dispatch),
          task.cause,
          dispatch.deferral,
      ) + task.then?.let { admit(it, task.assignee, task.cause, Deferral.NONE) }.orEmpty()

  internal fun removeTask(task: Task): TaskRemovedEvent {
    requireAtEnd()
    return gameWorld.apply(TaskRemovedEvent(gameWorld.nextOrdinal, task, transactionForEvent()))
  }

  /**
   * Makes the earliest remaining deferred work ordinary once no ordinary task remains: all `LATER`
   * work, or when none remains, all `FINALLY` work ([rules L2-18 and
   * L2-19](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md#2-instructions)).
   * Work deferred while that batch runs waits for the whole batch. A promoted dispatch is performed
   * at once, before it is removed, so its body joins the batch's work. Promotion is an ordinary
   * task edit, so recorded-event restoration also restores the previous deferral.
   *
   * [Transactions] calls this when a transaction completes successfully, so these edits belong to
   * the transaction that made the work eligible. Individual queue edits no longer promote: a
   * transaction that empties the queue only momentarily, before adding its own ordinary work, does
   * not promote deferred work it has not finished competing with.
   */
  internal fun promoteDeferredWork() {
    while (true) {
      val pending = gameWorld.tasks.extract { it }
      if (pending.isEmpty() || pending.any { !it.deferred }) return
      val earliest = pending.minOf { it.deferral }
      val batch = pending.filter { it.deferral == earliest }
      batch.forEach { editTask(it.copy(deferral = Deferral.NONE)) }
      val dispatches =
          batch
              .map { gameWorld.tasks.getTaskData(it.id) }
              .filter { it.instruction is Instruction.Dispatch }
      if (dispatches.isEmpty()) return
      for (task in dispatches) {
        perform(task.instruction as Instruction.Dispatch, task)
        gameWorld.apply(TaskRemovedEvent(gameWorld.nextOrdinal, task, transactionForEvent()))
      }
    }
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
    requireAtEnd()
    val normalized = normalizeTask(newTask, isAbstract)
    val oldTask = gameWorld.tasks.getTaskData(normalized.id)
    if (normalized == oldTask) return null
    return gameWorld.apply(
        TaskEditedEvent(
            gameWorld.nextOrdinal,
            oldTask = oldTask,
            task = normalized,
            transaction = transactionForEvent(),
        )
    )
  }

  internal fun normalizeForSelection(task: Task): Task =
      assignConcreteChangeToAdmin(normalizeTask(task, isAbstract))

  internal fun assigneeAfterContextualSelection(task: Task): Actor {
    val selected = task.copy(selected = true)
    return normalizeForSelection(selected).assignee
  }

  private fun assignConcreteChangeToAdmin(task: Task): Task {
    if (task.assignee == ADMIN) return task
    if (task.instruction.isAbstract(gameWorld.reader)) return task
    fun change(instruction: InstructionTree): Change? =
        when (instruction) {
          is Change -> instruction
          is By -> change(instruction.inner)
          is Per -> change(instruction.inner)
          is Gated -> change(instruction.inner)
          else -> null
        }
    val change = change(task.instruction) ?: return task
    if (change.count == ActualScalar(0)) return task
    val affected = change.gaining ?: change.removing ?: return task
    val affectedType = classTable.resolve(affected)
    if (!affectedType.rootClass.isSubtypeOf(systemClass)) {
      fun explicitlyByAdmin(instruction: InstructionTree): Boolean =
          when (instruction) {
            is By -> gameWorld.reader.resolve(instruction.actor).expression == ADMIN.expression
            is Gated -> explicitlyByAdmin(instruction.inner)
            else -> false
          }
      // TODO(ENGINE_CONTRACT): review concrete BY Admin routing for removals, transmutations, and
      // wrappers too. Admission currently recognizes only pure non-System gains through BY/Gated;
      // other explicit Admin work can stay assigned to the Player until selection.
      if (change.removing == null && explicitlyByAdmin(task.instruction)) {
        return task.copy(assignee = ADMIN)
      }
      return task
    }
    // The System step belongs to Admin, but completing it must return later work to the actor
    // who had the sequence before this handoff. Encode that recipient in the continuation itself.
    return task.copy(
        assignee = ADMIN,
        then =
            task.then?.let {
              InstructionGroup.of(
                  Instruction.Dispatch(
                      task.assignee.expression,
                      InstructionGroup.createTree(it.instructions),
                  )
              )
            },
    )
  }

  override fun toString(): String = gameWorld.tasks.toString()
}
