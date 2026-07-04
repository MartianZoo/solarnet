package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.TEMPORARY
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Player
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
    private val reader: GameReader,
    private val timeline: Timeline,
    private val player: Player,
    private val instructor: Instructor,
    private val changer: Changer,
) {

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
    // `manual` is a whole-game operation today: starting one while any queue is pending would
    // interleave with an existing operation and change current behavior.
    tasks.requireAllQueuesEmpty()
    addTasks(initialInstruction).forEach(::doTask)
    complete(autoExec, body)
  }

  internal fun beginManual(initialInstruction: Instruction, autoExec: AutoExecMode, body: () -> Unit) {
    // `beginManual` starts a whole-game manual operation, so pending tasks in any queue must block it.
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
    // Completion still means the whole operation drained; leaving another player's task pending
    // would be an observable behavior change from the old single-queue model.
    tasks.requireAllQueuesEmpty()
    require(reader.has(parse("MAX 0 $TEMPORARY")))
  }

  @Suppress("ControlFlowWithEmptyBody")
  internal fun autoExecNow(mode: AutoExecMode) {
    while (autoExecNext(mode)) {}
  }

  private fun autoExecNext(mode: AutoExecMode): Boolean /* should we continue */ {
    // Auto-exec intentionally preserves old global behavior: Engine/setup effects can enqueue
    // tasks for human players, and those must still be considered by the same auto-exec pass.
    if (mode == NONE || tasks.areAllQueuesEmpty()) return false

    // Prepared tasks freeze all game state, not just one player's queue, so the prepared-task check
    // and fallback candidate scan must remain global.
    val options: List<TaskId> =
        tasks.preparedTaskInAnyQueue()?.let(::listOf)
            ?: tasks.idsInAllQueues().filter(::canPrepareAnyTask)

    when (options.size) {
      // If auto-exec cannot prepare any global candidate, this preserves the old failure path by
      // asking the oldest global task to explain why preparation failed.
      0 -> prepareAnyTask(tasks.idsInAllQueues().first()).also { error("that should've failed") }
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

    // we're in unsafe mode. last resort: do the first task that executes

    var recoverable = false

    for (taskId in options) {
      try {
        timeline.atomic { doAnyTask(taskId) }
        return true
      } catch (e: AbstractException) {
        // we're in trouble if ALL of these are NotNowExceptions
        recoverable = true
        explainAnyTask(taskId, "abstract")
      } catch (e: NotNowException) {
        // we're in trouble if ALL of these are NotNowExceptions
        explainAnyTask(taskId, "currently impossible")
      }
    }
    if (!recoverable) throw DeadEndException("")

    return false // presumably everything is abstract
  }

  private fun explainTask(taskId: TaskId, reason: String) {
    tasks.editTask(tasks.getTaskData(taskId).copy(whyPending = reason))
  }

  private fun explainAnyTask(taskId: TaskId, reason: String) {
    // Auto-exec may be explaining a task from a different player queue than this Gameplay instance;
    // read globally, then mutate through the owning scoped queue for validation.
    val task = tasks.getTaskDataInAnyQueue(taskId)
    tasks.queueFor(task.owner).editTask(task.copy(whyPending = reason))
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(taskId: TaskId) {
    handleTask(tasks.getTaskData(taskId))
  }

  private fun handleAnyTask(taskId: TaskId) {
    // Auto-exec can execute tasks from any queue, so it must resolve the owner before removing the
    // task and enqueuing its follow-up through that owner's scoped queue.
    handleTask(tasks.getTaskDataInAnyQueue(taskId))
  }

  private fun handleTask(task: Task) {
    // Follow-up tasks inherit the completed task's owner even when this helper is called from a
    // global auto-exec path, so mutation must go through that owner's scoped queue.
    task.then?.let { tasks.queueFor(task.owner).addTasks(split(it), task.cause) }
    tasks.queueFor(task.owner).removeTask(task.id)
  }

  private fun dontCutTheLine(taskId: TaskId) {
    // `Task.next` is a global game-state lock: once any task is prepared, no other queue may act
    // until that prepared task is completed.
    val already = tasks.preparedTaskInAnyQueue()
    if (already != null && already != taskId) {
      val instr = tasks.getTaskDataInAnyQueue(already).instruction
      throw TaskException("task $already ($instr) is already prepared and must be executed first")
    }
  }

  // TURNS LAYER

  internal fun startTurn() = execute("NewTurn<$player>!")

  // GAMES LAYER

  internal fun reviseTask(taskId: TaskId, revised: Instruction) {
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

  internal fun canPrepareTask(taskId: TaskId): Boolean {
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

  private fun canPrepareAnyTask(taskId: TaskId): Boolean {
    // TODO better way
    dontCutTheLine(taskId)
    // Auto-exec scans all queues to preserve old single-queue behavior, so candidate preparation
    // has to read the candidate by global id.
    val unprepared = tasks.getTaskDataInAnyQueue(taskId).instruction
    return try {
      timeline.atomic { instructor.prepare(unprepared) }
      true
    } catch (e: Exception) {
      false
    }
  }

  internal fun prepareTask(taskId: TaskId): TaskId? =
      doPrepare(tasks.getTaskData(taskId)).also { lookAheadForTrouble(taskId) }

  private fun prepareAnyTask(taskId: TaskId): TaskId? =
      // Auto-exec prepares global candidates, including tasks owned by players other than this
      // scoped Gameplay instance.
      doPrepare(tasks.getTaskDataInAnyQueue(taskId)).also { lookAheadForTroubleInAnyQueue(taskId) }

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
    // The auto-exec lookahead rolls back immediately, but it must still simulate the global
    // candidate that auto-exec is considering.
    if (tasks.containsInAnyQueue(taskId)) {
      try {
        timeline.atomic {
          doAnyTask(taskId)
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
    // The replacement is always in the original owner's queue; use that scoped queue even when
    // preparation was requested from a global auto-exec path.
    return tasks.queueFor(task.owner).preparedTask()
  }

  private fun replace1WithN(replacement: Task) {
    val split = split(replacement.instruction)
    if (split.size == 1) {
      val one = split.instructions[0]
      // Prepared/revised tasks can come from auto-exec's global path, so mutation is routed through
      // the task owner's scoped queue instead of this Gameplay's queue.
      tasks.queueFor(replacement.owner).editTask(replacement.copy(instructionIn = one))
    } else {
      // Splitting a task preserves its owner; enqueue the split tasks in that owner's scoped queue.
      tasks.queueFor(replacement.owner).addTasks(split, replacement.cause)
      // Auto-exec can split a task owned by another player, so remove the original through that
      // same owner queue rather than this Gameplay's queue.
      handleTask(tasks.queueFor(replacement.owner).getTaskData(replacement.id))
    }
  }

  internal fun doFirstTask(revised: Instruction? = null) {
    val id = tasks.ids().min()
    prepareTask(id)
    if (id in tasks && revised != null) reviseTask(id, revised)
    if (id in tasks) doTask(id)
  }

  internal fun doTask(taskId: TaskId) {
    val prepared = doPrepare(tasks.getTaskData(taskId)) ?: return
    val preparedTask = tasks.getTaskData(prepared)
    val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
    // Effects can create tasks for their own owners; each new task is added through its owning
    // queue instead of assuming it belongs to this Gameplay's player.
    newTasks.forEach { tasks.queueFor(it.owner).addTasks(it) }
    handleTask(taskId)
  }

  private fun doAnyTask(taskId: TaskId) {
    // Auto-exec executes global candidates, so it must read the selected task by global id.
    val prepared = doPrepare(tasks.getTaskDataInAnyQueue(taskId)) ?: return
    // Preparation can replace/split the original task, so the prepared id also has to be resolved
    // globally in the auto-exec path.
    val preparedTask = tasks.getTaskDataInAnyQueue(prepared)
    val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
    // The new tasks produced by executing a global candidate still have explicit owners; route each
    // mutation through its owner queue.
    newTasks.forEach { tasks.queueFor(it.owner).addTasks(it) }
    handleAnyTask(taskId)
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

  internal fun tryTask(id: TaskId) {
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

  internal fun tryTask(revised: Instruction) {
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
  internal fun tryPreparedTask(): Boolean /* did I do stuff? */ {
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

  private fun tryPreparedAnyTask(): Boolean /* did I do stuff? */ {
    // Auto-exec must honor a prepared task in any queue because `next` freezes the whole game state.
    val taskId = tasks.preparedTaskInAnyQueue()!!
    return try {
      doAnyTask(taskId)
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (e: AbstractException) {
      explainAnyTask(taskId, "abstract")
      false
    }
  }

  private fun execute(instruction: String, fakeCause: Cause? = null): Unit =
      addTasks(parse(instruction), fakeCause).forEach(::doTask)
}
