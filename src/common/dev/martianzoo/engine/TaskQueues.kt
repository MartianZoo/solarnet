package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionGroup
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
 * * Normalization retains task identity, controller, selection assignee, current assignee,
 *   selection, and cause. Admission and contextual selection assign concrete System gains and
 *   removals to Admin, including changes scaled by `Per`. Abstract changes remain for the current
 *   assignee to choose; explicit instruction-side `BY` remains authoritative. Selected tasks cannot
 *   be replaced by independent siblings
 */
internal class TaskQueues(private val gameWorld: GameWorld) {
  private val classTable: ClassTable = gameWorld.classTable
  private val systemClass = classTable.getClass(SYSTEM)
  private val isAbstract: (Expression) -> Boolean = { expression ->
    classTable.resolve(expression).abstract
  }

  internal fun addTasks(task: PendingTask) =
      addTasks(
          task.instruction,
          task.controller,
          task.cause,
          task.selectionAssignee,
      )

  internal fun addTasks(
      instruction: InstructionGroup,
      controller: Actor,
      cause: Cause?,
      selectionAssignee: Actor = controller,
  ): List<TaskAddedEvent> {
    val newTasks =
        newTasks(
                firstId = TaskId(gameWorld.nextOrdinal),
                controller = controller,
                instruction = instruction,
                cause = cause,
                selectionAssignee = selectionAssignee,
                isAbstract = isAbstract,
            )
            .map(::assignSystemChangeToAdmin)
    return newTasks.map {
      require(it.id.ordinal == gameWorld.nextOrdinal)
      gameWorld.apply(TaskAddedEvent(gameWorld.nextOrdinal, it))
    }
  }

  internal fun removeTask(task: Task): TaskRemovedEvent {
    return gameWorld.apply(TaskRemovedEvent(gameWorld.nextOrdinal, task))
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
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
    val selected = task.copy(assignee = task.selectionAssignee, selected = true)
    return normalizeForSelection(selected).assignee
  }

  private fun assignSystemChangeToAdmin(task: Task): Task {
    if (task.instruction.isAbstract(gameWorld.reader)) return task
    fun change(instruction: Instruction): Change? =
        when (instruction) {
          is Change -> instruction
          is By -> change(instruction.inner)
          is Per -> change(instruction.inner)
          else -> null
        }
    val change = change(task.instruction) ?: return task
    val affected = change.gaining ?: change.removing ?: return task
    val affectedType = classTable.resolve(affected)
    return if (affectedType.rootClass.isSubtypeOf(systemClass)) task.copy(assignee = ADMIN)
    else task
  }

  override fun toString(): String = gameWorld.tasks.toString()
}
