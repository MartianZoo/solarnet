package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.TEMPORARY
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal class Implementations(
    private val tasks: WritableTaskQueue,
    taskQueues: TaskQueues,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val actor: Actor,
    private val instructor: Instructor,
    private val changer: Changer,
) {
  // Auto-exec scans the whole game for compatibility with existing workflows, and Task.next is a
  // whole-game lock. Keep that global visibility as a queue view rather than exposing TaskQueues
  // storage.
  private val allTasks = taskQueues.all()

  // CHANGES LAYER

  internal fun sneak(changes: Instruction, cause: Cause? = null) {
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

  internal fun addTasks(instruction: Instruction, firstCause: Cause? = null): List<TaskId> {
    val prepped = split(instruction)
    return tasks.addTasks(prepped, firstCause).map { it.task.id }
  }

  internal fun dropTask(taskId: TaskId): TaskRemovedEvent = tasks.removeTask(taskId)

  // OPERATIONS LAYER

  internal fun manual(initialInstruction: Instruction, autoExec: AutoExecMode, body: () -> Unit) {
    tasks.requireAllQueuesEmpty()
    addTasks(initialInstruction).forEach(::doTask)
    complete(autoExec, body)
  }

  internal fun beginManual(
      initialInstruction: Instruction,
      autoExec: AutoExecMode,
      body: () -> Unit,
  ) {
    tasks.requireAllQueuesEmpty()
    addTasks(initialInstruction).forEach(::doTask)
    continueManual(autoExec, body)
  }

  internal fun continueManual(autoExec: AutoExecMode, body: () -> Unit) {
    autoExecNow(autoExec)
    body()
    autoExecNow(autoExec)
  }

  internal fun complete(autoExec: AutoExecMode, body: () -> Unit) {
    continueManual(autoExec, body)
    tasks.requireAllQueuesEmpty()
    require(reader.has(parse("MAX 0 $TEMPORARY")))
  }

  internal fun autoExecNow(mode: AutoExecMode) {
    while (autoExecNext(mode)) {}
  }

  @Suppress("CyclomaticComplexMethod") // TODO: improve this
  private fun autoExecNext(mode: AutoExecMode): Boolean /* should we continue */ {
    if (mode == NONE || allTasks.isEmpty()) return false

    val options: List<TaskId> =
        allTasks.preparedTask()?.let(::listOf) ?: allTasks.ids().filter(::canPrepareAnyTask)

    when (options.size) {
      0 -> prepareAnyTask(allTasks.ids().first()).also { error("that should've failed") }
      1 -> {
        val taskId = options.single()
        prepareAnyTask(taskId) ?: return true
        try {
          if (tryPreparedAnyTask()) return true // if this fails we should fail too
        } catch (e: DeadEndException) {
          throw e.cause ?: e
        }
      }
      else -> if (mode == SAFE) return false
    }

    // We're in unsafe mode. Arbitrarily try tasks in stable iteration order.

    var recoverable = false

    for (taskId in options) {
      try {
        timeline.atomic { doAnyTask(taskId) }
        return true
      } catch (_: AbstractException) {
        // we're in trouble if ALL of these are NotNowExceptions
        recoverable = true
        explainAnyTask(taskId, "abstract")
      } catch (_: NotNowException) {
        // we're in trouble if ALL of these are NotNowExceptions
        explainAnyTask(taskId, "currently impossible")
      }
    }
    if (!recoverable) throw DeadEndException("")

    return false // presumably everything is abstract
  }

  private fun explainTask(taskId: TaskId, reason: String) {
    explainTask(tasks, taskId, reason)
  }

  private fun explainAnyTask(taskId: TaskId, reason: String) =
      explainTask(queueForAnyTask(taskId), taskId, reason)

  private fun explainTask(queue: WritableTaskQueue, taskId: TaskId, reason: String) {
    queue.editTask(queue.getTaskData(taskId).copy(whyPending = reason))
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(queue: WritableTaskQueue, task: Task) {
    task.then?.let {
      queue.queueFor(task.assignee).addTasks(split(it), task.cause)
    }
    queue.removeTask(task.id)
  }

  private fun dontCutTheLine(taskId: TaskId) {
    // Task.next remains a global game-state lock; a scoped queue could miss a prepared task in
    // another player's queue and allow a caller to cut in front of it.
    val already = allTasks.preparedTask()
    if (already != null && already != taskId) {
      val instr = allTasks.getTaskData(already).instruction
      throw TaskException("task $already ($instr) is already prepared and must be executed first")
    }
  }

  // TURNS LAYER

  internal fun startTurn() = execute("NewTurn<$actor>!")

  // GAMES LAYER

  internal fun reviseTask(taskId: TaskId, revised: Instruction) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("$actor can't revise a task assigned to ${task.assignee}")
    }

    if (revised != task.instruction) {
      revised.ensureNarrows(task.instruction, reader)
      val replacement = if (task.next) instructor.prepare(revised) else revised
      replace1WithN(tasks, tasks.getTaskData(taskId).copy(instructionIn = replacement))
    }
  }

  @Suppress("TooGenericExceptionCaught") // TODO narrow? log?
  internal fun canPrepareTask(taskId: TaskId): Boolean {
    // TODO better way
    dontCutTheLine(taskId)
    val unprepared = tasks.getTaskData(taskId).instruction
    return try {
      timeline.atomic { instructor.prepare(unprepared) }
      true
    } catch (_: Exception) {
      false
    }
  }

  internal fun prepareTask(taskId: TaskId): TaskId? =
      doPrepare(tasks, tasks.getTaskData(taskId)).also { lookAheadForTrouble(taskId) }

  @Suppress("TooGenericExceptionCaught") // TODO narrow? log?
  private fun canPrepareAnyTask(taskId: TaskId): Boolean {
    val queue = queueForAnyTask(taskId)
    dontCutTheLine(taskId)
    val unprepared = queue.getTaskData(taskId).instruction
    return try {
      timeline.atomic { instructor.prepare(unprepared) }
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun prepareAnyTask(taskId: TaskId): TaskId? {
    val queue = queueForAnyTask(taskId)
    return doPrepare(queue, queue.getTaskData(taskId)).also {
      lookAheadForTroubleInAnyQueue(taskId)
    }
  }

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

  private fun lookAheadForTroubleInAnyQueue(taskId: TaskId) {
    if (taskId in allTasks) {
      try {
        timeline.atomic {
          doAnyTask(taskId)
          throw AbstractException("just getting this to roll back")
        }
      } catch (ignore: AbstractException) { // the only failure that's expected/normal
      }
    }
  }

  private fun doPrepare(queue: WritableTaskQueue, task: Task): TaskId? {
    dontCutTheLine(task.id)
    val replacement = instructor.prepare(task.instruction)
    replace1WithN(queue, task.copy(instructionIn = replacement, next = true))
    return queue.preparedTask()
  }

  private fun replace1WithN(queue: WritableTaskQueue, replacement: Task) {
    val split = split(replacement.instruction)
    if (split.size == 1) {
      val one = split.instructions[0]
      queue.editTask(replacement.copy(instructionIn = one))
    } else {
      queue.queueFor(replacement.assignee).addTasks(split, replacement.cause)
      handleTask(queue, queue.getTaskData(replacement.id))
    }
  }

  internal fun doFirstTask(revised: Instruction? = null) {
    val id = tasks.ids().min()
    prepareTask(id)
    if (id in tasks && revised != null) reviseTask(id, revised)
    if (id in tasks) doTask(id)
  }

  internal fun doTask(taskId: TaskId) {
    doTask(tasks, taskId)
  }

  private fun doTask(queue: WritableTaskQueue, taskId: TaskId) {
    val prepared = doPrepare(queue, queue.getTaskData(taskId)) ?: return
    val preparedTask = queue.getTaskData(prepared)
    val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
    newTasks.forEach { queue.queueFor(it.assignee).addTasks(it) }
    handleTask(queue, queue.getTaskData(taskId))
  }

  private fun doAnyTask(taskId: TaskId) {
    doTask(queueForAnyTask(taskId), taskId)
  }

  internal fun doTask(revised: Instruction) {
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
      if (taskData.assignee != actor) return false
      if (revised.narrows(taskData.instruction, reader)) return true
      return try {
        revised.narrows(instructor.prepare(taskData.instruction), reader)
      } catch (_: NotNowException) {
        false
      }
    }

    return tasks.matching(::weCanReviseIt).singleOrNull()
        ?: throw TaskException("there wasn't exactly one matching task; tasks are:\n$tasks")
  }

  internal fun tryTask(id: TaskId) {
    try {
      timeline.atomic {
        prepareTask(id)
        if (id in tasks) doTask(id)
      }
    } catch (_: AbstractException) {
      explainTask(id, "abstract")
    } catch (_: NotNowException) {
      explainTask(id, "currently impossible")
    }
  }

  internal fun tryTask(revised: Instruction) {
    val id = matchingTask(revised)
    try {
      doTask(revised)
    } catch (_: AbstractException) {
      explainTask(id, "abstract")
    } catch (_: NotNowException) {
      explainTask(id, "currently impossible")
    }
  }

  // Similar to tryTask, but a NotNowException is unrecoverable in this case
  internal fun tryPreparedTask(): Boolean /* did I do stuff? */ {
    val taskId = tasks.preparedTask()!!
    return try {
      doTask(taskId)
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (_: AbstractException) {
      explainTask(taskId, "abstract")
      false
    }
  }

  private fun tryPreparedAnyTask(): Boolean /* did I do stuff? */ {
    val taskId = allTasks.preparedTask()!!
    return try {
      doAnyTask(taskId)
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (_: AbstractException) {
      explainAnyTask(taskId, "abstract")
      false
    }
  }

  private fun queueForAnyTask(taskId: TaskId): WritableTaskQueue =
      tasks.queueFor(allTasks.getTaskData(taskId).assignee)

  private fun execute(instruction: String, fakeCause: Cause? = null): Unit =
      addTasks(parse(instruction), fakeCause).forEach(::doTask)
}
