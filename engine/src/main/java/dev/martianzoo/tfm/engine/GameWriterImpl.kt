package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.Transformers
import javax.inject.Inject

internal class GameWriterImpl @Inject constructor(
    private val tasks: WritableTaskQueue,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val player: Player,
    val instructor: Instructor,
    private val changer: Changer,
    transformers: Transformers,
) : GameWriter, UnsafeGameWriter {

  override fun addTask(instruction: Instruction, firstCause: Cause?) =
      timeline.atomic {
        val prepped = Instruction.split(preprocess(instruction))
        tasks.addTasks(prepped, player, firstCause)
      }

  override fun narrowTask(taskId: TaskId, narrowed: Instruction): TaskResult {
    val task = tasks.getTaskData(taskId)
    if (player != task.owner) {
      throw TaskException("$player can't narrow a task owned by ${task.owner}")
    }

    val current = task.instruction
    val fixedNarrowing = preprocess(narrowed)
    if (fixedNarrowing == current) return TaskResult()
    fixedNarrowing.ensureNarrows(current, reader)

    val replacement = if (task.next) instructor.prepare(fixedNarrowing) else fixedNarrowing
    return editButCheckCardinality(tasks.getTaskData(taskId).copy(instructionIn = replacement))
  }

  override fun prepareTask(taskId: TaskId): TaskId? {
    val result = doPrepare(tasks.getTaskData(taskId))

    // let's just look ahead though
    if (taskId in tasks) {
      try {
        timeline.atomic {
          executeTask(taskId)
          throw AbstractException("just getting this to roll back")
        }
      } catch (ignore: AbstractException) { // the only failure that's expected/normal
      }
    }
    return result
  }

  private fun doPrepare(task: Task): TaskId? {
    dontCutTheLine(task.id)

    val replacement = instructor.prepare(task.instruction)
    editButCheckCardinality(task.copy(instructionIn = replacement, next = true))
    return tasks.preparedTask()
  }

  // Use this to edit a task if the replacement instruction might be NoOp, in which case the
  // task is handleTask'd instead.
  private fun editButCheckCardinality(replacement: Task): TaskResult {
    return timeline.atomic {
      val split = Instruction.split(replacement.instruction)
      if (split.size == 1) {
        val reason = replacement.whyPending?.let { "(was: $it)" }
        val one = split.instructions[0]
        tasks.editTask(replacement.copy(instructionIn = one, whyPending = reason))
      } else {
        tasks.addTasks(split, replacement.owner, replacement.cause)
        handleTask(replacement.id)
      }
    }
  }

  override fun canPrepareTask(taskId: TaskId): Boolean {
    dontCutTheLine(taskId)
    val unprepared = tasks.getTaskData(taskId).instruction
    return try {
      instructor.prepare(unprepared)
      true
    } catch (e: Exception) {
      false
    }
  }

  override fun explainTask(taskId: TaskId, reason: String) {
    tasks.editTask(tasks.getTaskData(taskId).copy(whyPending = reason))
  }

  override fun executeTask(taskId: TaskId): TaskResult {
    return timeline.atomic {
      val prepared = doPrepare(tasks.getTaskData(taskId)) ?: return@atomic
      val preparedTask = tasks.getTaskData(prepared)
      val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
      newTasks.forEach(tasks::addTasks)
      handleTask(taskId)
    }
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    task.then?.let { tasks.addTasks(Instruction.split(it), task.owner, task.cause) }
    dropTask(taskId)
  }

  private fun dontCutTheLine(taskId: TaskId) {
    val already = tasks.preparedTask()
    if (already != null && already != taskId) {
      throw TaskException("task $already is already prepared and must be executed first")
    }
  }

  override fun unsafe(): UnsafeGameWriter = this

  override fun dropTask(taskId: TaskId) = tasks.removeTask(taskId)

  override fun sneak(changes: String, cause: Cause?) =
      sneak(preprocess(Parsing.parse(changes)), cause)

  // TODO: in theory any instruction would be sneakable, and it only means disabling triggers
  override fun sneak(changes: Instruction, cause: Cause?): TaskResult {
    return timeline.atomic {
      Instruction.split(changes).map {
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
  }

  private val xer = chain(transformers.standardPreprocess(), replaceOwnerWith(player))

  internal fun <P : PetElement> preprocess(node: P) = xer.transform(node)
}
