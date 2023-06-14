package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.PlayerScoped
import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.SpecialClassNames.TEMPORARY
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import javax.inject.Inject

@PlayerScoped
internal class Implementations
@Inject
constructor(
    private val tasks: WritableTaskQueue,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val player: Player,
    private val instructor: Instructor,
    private val changer: Changer,
) {

  // CHANGES LAYER

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

  // TASKS LAYER

  fun addTasks(instruction: Instruction, firstCause: Cause? = null): List<TaskId> {
    val prepped = split(instruction)
    return tasks.addTasks(prepped, player, firstCause).map { it.task.id }
  }

  fun dropTask(taskId: TaskId): TaskRemovedEvent = tasks.removeTask(taskId)

  // OPERATIONS LAYER

  fun manual(initialInstruction: Instruction, autoExec: AutoExecMode, body: () -> Unit) {
    require(tasks.isEmpty()) { tasks }
    addTasks(initialInstruction).forEach(::doTask)
    complete(autoExec, body)
  }

  fun beginManual(initialInstruction: Instruction, autoExec: AutoExecMode, body: () -> Unit) {
    require(tasks.isEmpty()) { tasks }
    addTasks(initialInstruction).forEach(::doTask)
    autoExecNow(autoExec)
    body()
  }

  fun complete(autoExec: AutoExecMode, body: () -> Unit) {
    autoExecNow(autoExec)
    body()
    autoExecNow(autoExec)

    require(tasks.isEmpty()) {
      "Should be no tasks left, but:\n" + this.tasks.extract { it }.joinToString("\n")
    }
    require(reader.has(parse("MAX 0 $TEMPORARY"))) // TODO make game rules do this
  }

  @Suppress("ControlFlowWithEmptyBody")
  fun autoExecNow(mode: AutoExecMode) {
    while (autoExecNext(mode)) {}
  }

  private fun autoExecNext(mode: AutoExecMode): Boolean /* should we continue */ {
    if (mode == NONE || tasks.isEmpty()) return false

    // see if we can prepare a task
    val options: List<TaskId> =
        tasks.preparedTask()?.let(::listOf) ?: tasks.ids().filter(::canPrepareTask)

    when (options.size) {
      0 -> prepareTask(tasks.ids().first()).also { error("that should've failed") }
      1 -> {
        val taskId = options.single()
        prepareTask(taskId) ?: return true
        try {
          if (tryPreparedTask()) return true // if this fails we should fail too
        } catch (e: DeadEndException) {
          throw e.cause ?: e
        }
      }
      else -> if (mode == SAFE) return false
    }

    // we're in unsafe mode. last resort: do the first task that executes

    var recoverable = false

    for (taskId in options) {
      try {
        timeline.atomic { doTask(taskId) }
        return true
      } catch (e: AbstractException) {
        // we're in trouble if ALL of these are NotNowExceptions
        recoverable = true
        explainTask(taskId, "abstract")
      } catch (e: NotNowException) {
        // we're in trouble if ALL of these are NotNowExceptions
        explainTask(taskId, "currently impossible")
      }
    }
    if (!recoverable) throw DeadEndException("")

    return false // presumably everything is abstract
  }

  private fun explainTask(taskId: TaskId, reason: String) {
    tasks.editTask(tasks.getTaskData(taskId).copy(whyPending = reason))
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
      val instr = tasks.getTaskData(already).instruction
      throw TaskException("task $already ($instr) is already prepared and must be executed first")
    }
  }

  // TURNS LAYER

  fun startTurn() = execute("NewTurn<$player>!")

  // GAMES LAYER

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
    // TODO better way
    dontCutTheLine(taskId)
    val unprepared = tasks.getTaskData(taskId).instruction
    return try {
      timeline.atomic { instructor.prepare(unprepared) }
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
          doTask(taskId)
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

  fun doFirstTask(revised: Instruction? = null) {
    val id = tasks.ids().min()
    prepareTask(id)
    if (id in tasks && revised != null) reviseTask(id, revised)
    if (id in tasks) doTask(id)
  }

  fun doTask(taskId: TaskId) {
    val prepared = doPrepare(tasks.getTaskData(taskId)) ?: return
    val preparedTask = tasks.getTaskData(prepared)
    val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
    newTasks.forEach(tasks::addTasks)
    handleTask(taskId)
  }

  fun doTask(revised: Instruction) {
    val id = matchingTask(revised)
    prepareTask(id)
    if (id in tasks) reviseTask(id, revised)
    if (id in tasks) doTask(id)
  }

  private fun matchingTask(revised: Instruction): TaskId {
    tasks.preparedTask()?.let {
      return it
    }

    fun weCanReviseIt(taskData: Task): Boolean {
      if (taskData.owner != player) return false
      if (revised.narrows(taskData.instruction, reader)) return true
      return try {
        revised.narrows(instructor.prepare(taskData.instruction), reader)
      } catch (e: NotNowException) {
        false
      }
    }

    return tasks.matching(::weCanReviseIt).singleOrNull()
        ?: throw TaskException("there wasn't exactly one matching task; tasks are:\n$tasks")
  }

  fun tryTask(id: TaskId) {
    try {
      timeline.atomic {
        prepareTask(id)
        if (id in tasks) doTask(id)
      }
    } catch (e: AbstractException) {
      explainTask(id, "abstract")
    } catch (e: NotNowException) {
      explainTask(id, "currently impossible")
    }
  }

  fun tryTask(revised: Instruction) {
    val id = matchingTask(revised)
    try {
      doTask(revised)
    } catch (e: AbstractException) {
      explainTask(id, "abstract")
    } catch (e: NotNowException) {
      explainTask(id, "currently impossible")
    }
  }

  // Similar to tryTask, but a NotNowException is unrecoverable in this case
  fun tryPreparedTask(): Boolean /* did I do stuff? */ {
    val taskId = tasks.preparedTask()!!
    return try {
      doTask(taskId)
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (e: AbstractException) {
      explainTask(taskId, "abstract")
      false
    }
  }

  private fun execute(instruction: String, fakeCause: Cause? = null): Unit =
      addTasks(parse(instruction), fakeCause).forEach(::doTask) // TODO where to share this?
}
