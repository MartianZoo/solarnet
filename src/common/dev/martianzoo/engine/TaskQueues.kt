package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable
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
 * * `Die >> b` or `a >> Die` produces [DeadEndException]
 * * `a, b >> null` is split into `a >> null` and `b >> null`
 * * `a, b >> c` produces some exception (which?)
 * * `a THEN b >> null` where `a THEN b` is separable is rewritten to `a >> b`
 * * `a THEN b >> c` where `a THEN b` is separable is rewritten to `a >> b THEN c`
 * * `a, Ok` becomes `a`
 * * `a, Die` becomes `Die`
 * * `a OR Die` becomes `a`; if every option is `Die`, the task produces [DeadEndException]
 * * A concrete selected task is guaranteed to execute successfully
 * * New tasks created have the same controller, Actor, and cause as the original. Selected tasks
 *   cannot be split
 */
internal class TaskQueues(
    private val gameWorld: GameWorld,
    private val classTable: ClassTable? = null,
) {
  private val isAbstract: ((Expression) -> Boolean)? = classTable?.let { table ->
    { expression -> table.resolve(expression).abstract }
  }

  internal fun addTasks(task: PendingTask) =
      addTasks(
          task.instruction,
          task.controller,
          task.cause,
          task.actor,
      )

  internal fun addTasks(
      instruction: InstructionGroup,
      controller: Actor,
      cause: Cause?,
      actor: Actor = controller,
  ): List<TaskAddedEvent> {
    val newTasks =
        newTasks(
            firstId = TaskId(gameWorld.nextOrdinal),
            controller = controller,
            instruction = instruction,
            cause = cause,
            actor = actor,
            isAbstract = isAbstract,
        )
    return newTasks.map {
      require(it.id.ordinal == gameWorld.nextOrdinal)
      gameWorld.apply(TaskAddedEvent(gameWorld.nextOrdinal, it))
    }
  }

  internal fun removeTask(task: Task): TaskRemovedEvent {
    return gameWorld.apply(TaskRemovedEvent(gameWorld.nextOrdinal, task))
  }

  internal fun editTask(newTask: Task): TaskEditedEvent? {
    val normalized = normalizeTask(newTask)
    val oldTask = gameWorld.tasks.getTaskData(normalized.id)
    if (normalized == oldTask) return null
    return gameWorld.apply(
        TaskEditedEvent(gameWorld.nextOrdinal, oldTask = oldTask, task = normalized)
    )
  }

  override fun toString(): String = gameWorld.tasks.toString()
}
