package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Deferral
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionGroup
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

/**
 * Constructs and edits task data using engine normalization before recording exact state events.
 * Here, `a >> b` is a task whose [Task.instruction] is `a` and whose [Task.then] is `b`.
 * * Removing task `a >> b` first creates task `b >> null`
 * * `Ok >> b` is removed
 * * Mandatory `Die >> b` or `a >> Die` produces [DeadEndException]; nonmandatory `Die` is `Ok`
 * * `a, b >> null` is split into `a >> null` and `b >> null`
 * * `a, b >> c` produces some exception (which?)
 * * `a THEN b >> null` where `a THEN b` is separable is rewritten to `a >> b`
 * * `a THEN b >> c` retains both boundaries rather than merging their independent variable scopes
 * * `a, Ok` becomes `a`
 * * `a, Die!` becomes `Die!`
 * * `a OR Die!` becomes `a`; if every option is mandatory `Die`, the task produces
 *   [DeadEndException]
 * * A concrete selected task is guaranteed to execute successfully
 * * Normalization retains task identity, current assignee, selection, and cause. Admission and
 *   contextual selection assign concrete System gains and removals to Admin, including changes
 *   scaled by `Per`. When that changes the assignee, a continuation dispatches back to the former
 *   assignee after Admin finishes the System change. Abstract changes remain for the current
 *   assignee to choose; explicit instruction-side `BY` remains authoritative. Selected tasks cannot
 *   be replaced by independent siblings
 * * Once no ordinary task remains, every task with the earliest remaining [Task.deferral] becomes
 *   ordinary at once
 * * An ordinary task whose instruction is a dispatch is performed at once: `TASK R { b } >> c`
 *   becomes task `b` for R, with the dispatch's deferral, and task `c`
 */
internal class TaskQueues(
    private val gameWorld: GameWorld,
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
    return admit(instruction, assignee, cause, deferral).also { promoteDeferredWork() }
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
            assignSystemChangeToAdmin(
                newTask(TaskId(gameWorld.nextOrdinal), assignee, it, cause, isAbstract, deferral)
            )
        val dispatch = task.instruction as? Instruction.Dispatch
        if (dispatch != null && !task.deferred) {
          perform(dispatch, task)
        } else {
          listOf(gameWorld.apply(TaskAddedEvent(gameWorld.nextOrdinal, task)))
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
    return gameWorld.apply(TaskRemovedEvent(gameWorld.nextOrdinal, task)).also {
      promoteDeferredWork()
    }
  }

  /**
   * Makes the earliest remaining deferred work ordinary once no ordinary task remains: all `LATER`
   * work, or when none remains, all `FINALLY` work ([rules L2-18 and
   * L2-19](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md#2-instructions)).
   * Work deferred while that batch runs waits for the whole batch. A promoted dispatch is performed
   * at once, before it is removed, so its body joins the batch's work. Promotion is an ordinary
   * task edit, so rollback and replay restore it with its transaction.
   */
  private fun promoteDeferredWork() {
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
        gameWorld.apply(TaskRemovedEvent(gameWorld.nextOrdinal, task))
      }
    }
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
    requireAtEnd()
    val normalized = normalizeTask(newTask, isAbstract)
    val oldTask = gameWorld.tasks.getTaskData(normalized.id)
    if (normalized == oldTask) return null
    return gameWorld.apply(
        TaskEditedEvent(gameWorld.nextOrdinal, oldTask = oldTask, task = normalized)
    )
  }

  internal fun normalizeForSelection(task: Task): Task =
      assignSystemChangeToAdmin(normalizeTask(task, isAbstract))

  internal fun assigneeAfterContextualSelection(task: Task): Actor {
    val selected = task.copy(selected = true)
    return normalizeForSelection(selected).assignee
  }

  private fun assignSystemChangeToAdmin(task: Task): Task {
    if (task.assignee == ADMIN) return task
    if (task.instruction.isAbstract(gameWorld.reader)) return task
    fun change(instruction: Instruction): Change? =
        when (instruction) {
          is Change -> instruction
          is By -> change(instruction.inner)
          is Per -> change(instruction.inner)
          else -> null
        }
    val change = change(task.instruction) ?: return task
    if (change.count == ActualScalar(0)) return task
    val affected = change.gaining ?: change.removing ?: return task
    val affectedType = classTable.resolve(affected)
    if (!affectedType.rootClass.isSubtypeOf(systemClass)) return task
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
