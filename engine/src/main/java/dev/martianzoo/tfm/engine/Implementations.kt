package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ComponentGraph.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Engine.PlayerScope
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import javax.inject.Inject

@PlayerScope
internal class Implementations
@Inject
constructor(
    private val tasks: WritableTaskQueue,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val player: Player,
    val instructor: Instructor,
    private val changer: Changer,
) {

  fun reviseTask(taskId: TaskId, revised: Instruction) {
    val task = tasks.getTaskData(taskId)
    if (player != task.owner) {
      throw TaskException("$player can't revise a task owned by ${task.owner}")
    }

    if (revised != task.instruction) {
      revised.ensureNarrows(task.instruction, reader)
      val replacement = if (task.next) instructor.prepare(revised) else revised
      replace1WithN(tasks.getTaskData(taskId).copy(instructionIn = replacement))
    }
  }

  fun canPrepareTask(taskId: TaskId): Boolean {
    dontCutTheLine(taskId)
    val unprepared = tasks.getTaskData(taskId).instruction
    return try {
      instructor.prepare(unprepared)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun prepareTask(taskId: TaskId): TaskId? =
      doPrepare(tasks.getTaskData(taskId)).also { lookAheadForTrouble(taskId) }

  private fun lookAheadForTrouble(taskId: TaskId) {
    if (taskId in tasks) {
      try {
        timeline.atomic {
          executeTask(taskId)
          throw AbstractException("just getting this to roll back")
        }
      } catch (ignore: AbstractException) { // the only failure that's expected/normal
      }
    }
  }

  private fun doPrepare(task: Task): TaskId? {
    dontCutTheLine(task.id)

    val replacement = instructor.prepare(task.instruction)
    replace1WithN(task.copy(instructionIn = replacement, next = true))
    return tasks.preparedTask()
  }

  // Use this to edit a task if the replacement instruction might be NoOp, in which case the
  // task is handleTask'd instead.
  private fun replace1WithN(replacement: Task) {
    val split = split(replacement.instruction)
    if (split.size == 1) {
      val reason = replacement.whyPending?.let { "(was: $it)" }
      val one = split.instructions[0]
      tasks.editTask(replacement.copy(instructionIn = one, whyPending = reason))
    } else {
      tasks.addTasks(split, replacement.owner, replacement.cause)
      handleTask(replacement.id)
    }
  }

  fun executeTask(taskId: TaskId) {
    val prepared = doPrepare(tasks.getTaskData(taskId)) ?: return
    val preparedTask = tasks.getTaskData(prepared)
    val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
    newTasks.forEach(tasks::addTasks)
    handleTask(taskId)
  }

  fun explainTask(taskId: TaskId, reason: String) {
    tasks.editTask(tasks.getTaskData(taskId).copy(whyPending = reason))
  }

  fun addTasks(instruction: Instruction, firstCause: Cause? = null) {
    val prepped = split(instruction)
    tasks.addTasks(prepped, player, firstCause)
  }

  fun dropTask(taskId: TaskId): TaskRemovedEvent = tasks.removeTask(taskId)

  fun sneak(changes: Instruction, cause: Cause? = null) {
    split(changes).map {
      val count = (it as Change).count as ActualScalar
      changer.change(
          count.value,
          it.gaining?.toComponent(reader),
          it.removing?.toComponent(reader),
          cause,
          orRemoveOneDependent = false,
      )
    }
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    task.then?.let { tasks.addTasks(split(it), task.owner, task.cause) }
    tasks.removeTask(taskId)
  }

  private fun dontCutTheLine(taskId: TaskId) {
    val already = tasks.preparedTask()
    if (already != null && already != taskId) {
      throw TaskException("task $already is already prepared and must be executed first")
    }
  }
}
