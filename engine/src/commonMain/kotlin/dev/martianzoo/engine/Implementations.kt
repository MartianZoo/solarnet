package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.Exceptions.abstractInstruction
import dev.martianzoo.api.Exceptions.orWithoutChoice
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
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal class Implementations(
    private val tasks: TaskQueue,
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
    split(changes).forEach {
      if (it is Instruction.Or) throw orWithoutChoice(it)
      val change =
          it as? Change ?: throw ExpressionException("sneak accepts only direct changes, not: $it")
      val count = change.count as? ActualScalar ?: throw abstractInstruction(change)
      changer.change(
          count.value,
          change.gaining?.toComponent(reader),
          change.removing?.toComponent(reader),
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

  internal fun dropTasks(): List<TaskRemovedEvent> = tasks.ids().map(tasks::removeTask)

  // OPERATIONS LAYER

  internal fun manual(initialInstruction: Instruction, autoExec: AutoExecMode, body: () -> Unit) {
    val preexistingTasks = allTasks.ids()
    allTasks.preparedTask()?.let {
      throw TaskException("can't start a manual operation while task $it is prepared")
    }
    addTasks(initialInstruction).forEach(::doInitialTask)
    complete(autoExec, preexistingTasks, body)
  }

  internal fun beginManual(
      initialInstruction: Instruction,
      autoExec: AutoExecMode,
      body: () -> Unit,
  ) {
    tasks.requireAllQueuesEmpty()
    addTasks(initialInstruction).forEach(::doInitialTask)
    continueManual(autoExec, body)
  }

  private fun doInitialTask(taskId: TaskId) {
    try {
      doTask(taskId)
    } catch (_: AbstractException) {
      explainTask(taskId, "abstract")
    }
  }

  internal fun continueManual(autoExec: AutoExecMode, body: () -> Unit) {
    autoExecNow(autoExec)
    body()
    autoExecNow(autoExec)
  }

  internal fun complete(
      autoExec: AutoExecMode,
      allowedPendingTasks: Set<TaskId> = emptySet(),
      body: () -> Unit,
  ) {
    continueManual(autoExec, body)
    val pending = allTasks.extract { it }.filter { it.id !in allowedPendingTasks }
    if (pending.isNotEmpty()) {
      if (pending.any { it.whyPending == "abstract" }) {
        throw AbstractException("pending abstract tasks:\n${pending.joinToString("\n")}")
      }
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
    if (!reader.has(parse("MAX 0 $TEMPORARY"))) {
      throw DeadEndException("temporary components remained after the operation")
    }
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

  private fun explainTask(queue: TaskQueue, taskId: TaskId, reason: String) {
    queue.editTask(queue.getTaskData(taskId).copy(whyPending = reason))
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(queue: TaskQueue, task: Task) {
    task.then?.let {
      queue.queueFor(task.assignee).addTasks(split(it), task.cause, task.actor)
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

    if (revised == task.instruction) return
    val directlyNarrows = revised.narrows(task.instruction, reader)
    val selectedThen =
        if (directlyNarrows) null else selectFirstStageOrNull(task.instruction, revised)
    if (selectedThen == null) revised.ensureNarrows(task.instruction, reader)

    // A selected group must split before its children are prepared against successive worlds.
    val replacement = if (task.next && revised !is Multi) instructor.prepare(revised) else revised
    val continuation =
        selectedThen?.let { Then.create(it.instructions.drop(1) + listOfNotNull(task.then)) }
            ?: task.then
    replace1WithN(
        tasks,
        task.copy(instructionIn = replacement, thenIn = continuation),
    )
  }

  internal fun reviseTask(current: Instruction, revised: Instruction) {
    reviseTask(taskWithInstruction(current), revised)
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

  internal fun prepareTask(instruction: Instruction): TaskId? =
      prepareTask(taskWithInstruction(instruction))

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

  private fun doPrepare(queue: TaskQueue, task: Task): TaskId? {
    dontCutTheLine(task.id)
    val replacement = instructor.prepare(task.instruction)
    replace1WithN(queue, task.copy(instructionIn = replacement, next = true))
    return queue.preparedTask()
  }

  private fun replace1WithN(queue: TaskQueue, replacement: Task) {
    val split = split(replacement.instruction)
    if (split.size == 1) {
      val one = split.instructions[0]
      queue.editTask(replacement.copy(instructionIn = one))
    } else {
      queue.queueFor(replacement.assignee).addTasks(split, replacement.cause, replacement.actor)
      handleTask(queue, queue.getTaskData(replacement.id))
    }
  }

  internal fun doTask(taskId: TaskId) {
    doTask(tasks, taskId)
  }

  private fun doTask(queue: TaskQueue, taskId: TaskId) {
    val prepared = doPrepare(queue, queue.getTaskData(taskId)) ?: return
    val preparedTask = queue.getTaskData(prepared)
    val newTasks =
        instructor.execute(preparedTask.instruction, preparedTask.cause, preparedTask.actor)
    newTasks.forEach { queue.queueFor(it.assignee).addTasks(it) }
    handleTask(queue, queue.getTaskData(taskId))
  }

  private fun doAnyTask(taskId: TaskId) {
    doTask(queueForAnyTask(taskId), taskId)
  }

  internal fun doTask(revised: Instruction, taskNumber: Int? = null) {
    val id = matchingTask(revised, taskNumber)
    prepareTask(id)
    if (id in tasks) reviseTask(id, revised)
    if (id in tasks) doTask(id)
  }

  private fun matchingTask(revised: Instruction, taskNumber: Int? = null): TaskId {
    tasks.preparedTask()?.let {
      return it
    }

    if (taskNumber != null) {
      if (taskNumber < 1) throw TaskException("task number must be at least 1")
      return tasks.ids().elementAtOrNull(taskNumber - 1)
          ?: throw TaskException("there is no task $taskNumber; tasks are:\n$tasks")
    }

    fun weCanReviseIt(taskData: Task): Boolean {
      if (taskData.assignee != actor) return false
      val instruction = taskData.instruction
      if (narrowsTask(revised, instruction)) return true
      return try {
        narrowsTask(revised, instructor.prepare(instruction))
      } catch (_: NotNowException) {
        false
      }
    }

    return uniqueMatchingTask(tasks.extract { it }.filter(::weCanReviseIt))
  }

  private fun narrowsTask(revised: Instruction, existing: Instruction): Boolean =
      revised.narrows(existing, reader) || selectFirstStageOrNull(existing, revised) != null

  private fun selectFirstStageOrNull(instruction: Instruction, revised: Instruction): Then? {
    if (revised is Then) return null
    val candidates: List<Pair<Then, Boolean>> =
        when (instruction) {
          is Then -> listOf(instruction to false)
          is Or -> instruction.instructions.filterIsInstance<Then>().map { it to true }
          else -> emptyList()
        }
    return candidates
        .mapNotNull { (then, selectedFromOr) ->
          try {
            val loweredBinding = loweredRemovalBinding(then.instructions.first(), revised)
            if (selectedFromOr) {
              then.selectFirstStage(revised, reader, loweredBinding)
            } else {
              then.bindFirstStage(revised, reader, loweredBinding)
            }
          } catch (_: NarrowingException) {
            null
          }
        }
        .singleOrNull()
  }

  private fun loweredRemovalBinding(wide: Instruction, narrow: Instruction): PetTransformer? {
    val general = (wide as? Change)?.removing ?: return null
    val specific = (narrow as? Change)?.removing ?: return null
    val transformers = (reader as GameReaderImpl).transformers
    return transformers.checkedSubstituter(reader.resolve(general), reader.resolve(specific))
  }

  private fun taskWithInstruction(instruction: Instruction): TaskId =
      uniqueMatchingTask(tasks.extract { it }.filter { it.instruction == instruction })

  private fun uniqueMatchingTask(matches: List<Task>): TaskId {
    val first =
        matches.firstOrNull()
            ?: throw TaskException("there wasn't exactly one matching task; tasks are:\n$tasks")
    // Origin metadata does not distinguish choices that otherwise present and behave identically.
    if (matches.map { it.copy(id = first.id, cause = first.cause) }.distinct().size == 1) {
      return first.id
    }
    throw TaskException("there wasn't exactly one matching task; tasks are:\n$tasks")
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

  internal fun tryTask(revised: Instruction, taskNumber: Int? = null) {
    val id = matchingTask(revised, taskNumber)
    try {
      doTask(revised, taskNumber)
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

  private fun queueForAnyTask(taskId: TaskId): TaskQueue =
      tasks.queueFor(allTasks.getTaskData(taskId).assignee)

  private fun execute(instruction: String, fakeCause: Cause? = null): Unit =
      addTasks(parse(instruction), fakeCause).forEach(::doTask)
}
