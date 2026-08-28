package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.Exceptions.abstractInstruction
import dev.martianzoo.pets.api.Exceptions.orWithoutChoice
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.TEMPORARY
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId

internal class Implementations(
    private val tasks: TaskQueue,
    taskQueues: TaskQueues,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val actor: Actor,
    private val instructor: Instructor,
    private val changer: Changer,
) {
  // Auto-exec scans the whole game for compatibility with existing workflows, and Task.selected is
  // a whole-game lock. Keep that global visibility as a queue view rather than exposing TaskQueues
  // storage.
  private val allTasks = taskQueues.all()

  private object SelectionProbeSucceeded : RuntimeException()

  // CHANGES LAYER

  internal fun sneak(changes: InstructionGroup, cause: Cause? = null) {
    changes.instructions.forEach {
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

  internal fun addTasks(instructions: InstructionGroup, firstCause: Cause? = null): List<TaskId> =
      tasks.addTasks(instructions, firstCause).map { it.task.id }

  internal fun dropTask(taskId: TaskId): TaskRemovedEvent = tasks.removeTask(taskId)

  internal fun dropTasks(): List<TaskRemovedEvent> = tasks.ids().map(tasks::removeTask)

  // OPERATIONS LAYER

  internal fun manual(
      initialInstructions: InstructionGroup,
      autoExec: AutoExecMode,
      body: () -> Unit,
  ) {
    val preexistingTasks = allTasks.ids()
    allTasks.selectedTask()?.let {
      throw TaskException("can't start a manual operation while task $it holds the select-lock")
    }
    addTasks(initialInstructions).forEach(::doInitialTask)
    complete(autoExec, preexistingTasks, body)
  }

  internal fun beginManual(
      initialInstructions: InstructionGroup,
      autoExec: AutoExecMode,
      body: () -> Unit,
  ) {
    tasks.requireAllQueuesEmpty()
    addTasks(initialInstructions).forEach(::doInitialTask)
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
      throw DeadEndException(
          "temporary components remained after the operation: " +
              reader.getComponents("Temporary").elements
      )
    }
  }

  internal fun autoExecNow(mode: AutoExecMode) {
    while (autoExecNext(mode)) {}
  }

  @Suppress("CyclomaticComplexMethod") // TODO: improve this
  private fun autoExecNext(mode: AutoExecMode): Boolean /* should we continue */ {
    if (mode == NONE || allTasks.isEmpty()) return false

    val options: List<TaskId> =
        allTasks.selectedTask()?.let(::listOf) ?: allTasks.ids().filter(::canSelectAnyTask)

    when (options.size) {
      0 -> doAnyTask(allTasks.ids().first()).also { error("that should've completed") }
      1 -> {
        val taskId = options.single()
        val queue = queueForAnyTask(taskId)
        selectTask(queue, queue.getTaskData(taskId)) ?: return true
        try {
          if (trySelectedAnyTask()) return true // if this fails we should fail too
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
        val task = queueForAnyTask(taskId).getTaskData(taskId)
        if (task.instruction.isAbstract(reader)) {
          recoverable = true
          explainAnyTask(taskId, "abstract")
        } else {
          // we're in trouble if ALL of these are NotNowExceptions
          explainAnyTask(taskId, "currently impossible")
        }
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
    task.then?.let { queue.queueFor(task.assignee).addTasks(it, task.cause, task.actor) }
    queue.removeTask(task.id)
  }

  private fun enforceSelectLock(taskId: TaskId) {
    // Selection is a global game-state lock; a scoped queue could miss the selected task in
    // another player's queue and allow a caller to cut in front of it.
    val already = allTasks.selectedTask()
    if (already != null && already != taskId) {
      val instr = allTasks.getTaskData(already).instruction
      throw TaskException("task $already ($instr) holds the select-lock and must finish first")
    }
  }

  // TURNS LAYER

  internal fun startTurn() = execute("NewTurn<$actor>!")

  // GAMES LAYER

  internal fun narrowTask(narrowing: InstructionTree, intensityOmitted: Boolean = false) {
    val taskId = tasks.selectedTask() ?: throw TaskException("$actor has no selected task")
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("$actor can't narrow a task assigned to ${task.assignee}")
    }

    val effectiveNarrowing = effectiveNarrowing(narrowing, task.instruction, intensityOmitted)
    if (effectiveNarrowing == task.instruction) {
      executeSelectedIfConcrete(tasks, taskId)
      return
    }
    val directlyNarrows = effectiveNarrowing.narrows(task.instruction, reader)
    val selectedThen =
        if (directlyNarrows) null else selectFirstStageOrNull(task.instruction, effectiveNarrowing)
    if (selectedThen == null) effectiveNarrowing.ensureNarrows(task.instruction, reader)

    if (selectedThen != null && task.then != null) {
      throw TaskException("can't select the first stage of a THEN with an outer continuation")
    }
    val continuation = selectedThen?.continuationAfterFirst() ?: task.then

    instructor.validateAmApSelection(task.instruction, effectiveNarrowing)
    // A selected group completes structurally before its children resolve against successive
    // worlds.
    val replacement =
        if (effectiveNarrowing is Instruction) instructor.resolve(effectiveNarrowing)
        else effectiveNarrowing
    replace1WithN(tasks, task, replacement, selected = true, then = continuation)
    if (taskId in tasks) executeSelectedIfConcrete(tasks, taskId)
  }

  @Suppress("TooGenericExceptionCaught") // TODO narrow? log?
  internal fun canSelectTask(taskId: TaskId): Boolean {
    return try {
      timeline.atomic {
        selectAndExecuteIfConcrete(tasks, taskId)
        throw SelectionProbeSucceeded
      }
      false
    } catch (_: SelectionProbeSucceeded) {
      true
    } catch (_: Exception) {
      false
    }
  }

  internal fun selectTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("$actor can't select a task assigned to ${task.assignee}")
    }
    selectAndExecuteIfConcrete(tasks, taskId)
  }

  internal fun selectTask(instruction: Instruction) = selectTask(taskWithInstruction(instruction))

  @Suppress("TooGenericExceptionCaught") // TODO narrow? log?
  private fun canSelectAnyTask(taskId: TaskId): Boolean {
    val queue = queueForAnyTask(taskId)
    return try {
      timeline.atomic {
        selectAndExecuteIfConcrete(queue, taskId)
        throw SelectionProbeSucceeded
      }
      false
    } catch (_: SelectionProbeSucceeded) {
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun selectAndExecuteIfConcrete(queue: TaskQueue, taskId: TaskId) {
    val selected = selectTask(queue, queue.getTaskData(taskId)) ?: return
    executeSelectedIfConcrete(queue, selected)
  }

  private fun executeSelectedIfConcrete(queue: TaskQueue, taskId: TaskId) {
    val task = queue.getTaskData(taskId)
    if (!task.instruction.isAbstract(reader)) {
      executeSelectedTask(queue, taskId)
    }
  }

  private fun selectTask(queue: TaskQueue, task: Task): TaskId? {
    enforceSelectLock(task.id)
    if (task.selected) return task.id
    val replacement = instructor.resolve(task.instruction)
    replace1WithN(queue, task, replacement, selected = true, then = task.then)
    return queue.selectedTask()
  }

  private fun replace1WithN(
      queue: TaskQueue,
      original: Task,
      replacement: InstructionTree,
      selected: Boolean,
      then: InstructionGroup?,
  ) {
    val group = InstructionGroup.of(replacement)
    if (group.size == 1) {
      val instruction = group.instructions.single()
      val updated =
          if (instruction is Then && then == null) {
            Task.newTasks(
                    original.id,
                    original.assignee,
                    group,
                    original.cause,
                    original.actor,
                    reader::isAbstract,
                )
                .single()
                .copy(selected = selected, whyPending = original.whyPending)
          } else {
            original.copy(instructionIn = instruction, selected = selected, thenIn = then)
          }
      queue.editTask(updated)
    } else {
      // Structural completion replaces the selected task with ordinary pending siblings. No child
      // inherits selection; a later player input must select whichever sibling comes next.
      queue.queueFor(original.assignee).addTasks(group, original.cause, original.actor)
      handleTask(queue, original.copy(thenIn = then))
    }
  }

  internal fun doTask(taskId: TaskId) {
    doTask(tasks, taskId)
  }

  private fun doTask(queue: TaskQueue, taskId: TaskId) {
    val selected = selectTask(queue, queue.getTaskData(taskId)) ?: return
    val selectedTask = queue.getTaskData(selected)
    if (selectedTask.instruction.isAbstract(reader)) {
      throw abstractInstruction(selectedTask.instruction)
    }
    executeSelectedTask(queue, selected)
  }

  private fun executeSelectedTask(queue: TaskQueue, taskId: TaskId) {
    val selectedTask = queue.getTaskData(taskId)
    check(selectedTask.selected)
    val newTasks =
        instructor.executeResolved(
            selectedTask.instruction,
            selectedTask.cause,
            selectedTask.actor,
        )
    newTasks.forEach { queue.queueFor(it.assignee).addTasks(it) }
    handleTask(queue, selectedTask)
  }

  private fun doAnyTask(taskId: TaskId) {
    doTask(queueForAnyTask(taskId), taskId)
  }

  internal fun doTask(
      narrowing: InstructionTree,
      taskNumber: Int? = null,
      intensityOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
  ) {
    val evaluated = evaluatePer(narrowing)
    val id = matchingTask(evaluated, taskNumber, intensityOmitted)
    val tasksBefore = tasks.ids()
    selectTask(tasks, tasks.getTaskData(id)) ?: return
    narrowTask(evaluated, intensityOmitted)
    if (id !in tasks) {
      if (executeSubmittedGroup) {
        tasks.ids().filter { it !in tasksBefore }.forEach(::doTask)
      }
      return
    }
    throw abstractInstruction(tasks.getTaskData(id).instruction)
  }

  private fun evaluatePer(instruction: InstructionTree): InstructionTree =
      if (instruction is Per) instructor.resolve(instruction) else instruction

  private fun matchingTask(
      narrowing: InstructionTree,
      taskNumber: Int? = null,
      intensityOmitted: Boolean = false,
  ): TaskId {
    tasks.selectedTask()?.let {
      return it
    }

    if (taskNumber != null) {
      if (taskNumber < 1) throw TaskException("task number must be at least 1")
      return tasks.ids().elementAtOrNull(taskNumber - 1)
          ?: throw TaskException("there is no task $taskNumber; tasks are:\n$tasks")
    }

    fun weCanNarrowIt(taskData: Task): Boolean {
      if (taskData.assignee != actor) return false
      val instruction = taskData.instruction
      if (narrowsTask(narrowing, instruction, intensityOmitted)) return true
      return try {
        narrowsTask(narrowing, instructor.resolve(instruction), intensityOmitted)
      } catch (_: NotNowException) {
        false
      }
    }

    return uniqueMatchingTask(tasks.extract { it }.filter(::weCanNarrowIt))
  }

  private fun narrowsTask(
      narrowing: InstructionTree,
      existing: InstructionTree,
      intensityOmitted: Boolean,
  ): Boolean {
    val effectiveNarrowing = effectiveNarrowing(narrowing, existing, intensityOmitted)
    return effectiveNarrowing.narrows(existing, reader) ||
        selectFirstStageOrNull(existing, effectiveNarrowing) != null
  }

  private fun effectiveNarrowing(
      narrowing: InstructionTree,
      existing: InstructionTree,
      intensityOmitted: Boolean,
  ): InstructionTree {
    if (!intensityOmitted || narrowing !is Change) return narrowing
    if (narrowing.narrows(existing, reader)) return narrowing

    fun inheritIntensity(change: Change): InstructionTree =
        when (narrowing) {
          is Gain -> Gain.gain(narrowing.scaledEx, change.intensity)
          is Remove -> Remove.remove(narrowing.scaledEx, change.intensity)
          is Transmute -> narrowing.copy(intensity = change.intensity)
        }

    val choices =
        when (existing) {
          is Change -> listOf(existing)
          is Or -> existing.instructions.filterIsInstance<Change>()
          else -> emptyList()
        }
    return choices
        .mapNotNull { choice ->
          inheritIntensity(choice).takeIf { inherited -> inherited.narrows(choice, reader) }
        }
        .distinct()
        .singleOrNull() ?: narrowing
  }

  private fun selectFirstStageOrNull(
      instruction: InstructionTree,
      narrowing: InstructionTree,
  ): Then? {
    if (narrowing is Then) return null
    val revisedInstruction = narrowing as? Instruction ?: return null
    val candidates: List<Pair<Then, Boolean>> =
        when (instruction) {
          is Then -> listOf(instruction to false)
          is Or -> instruction.instructions.filterIsInstance<Then>().map { it to true }
          else -> emptyList()
        }
    return candidates
        .mapNotNull { (then, selectedFromOr) ->
          try {
            val loweredBinding = loweredRemovalBinding(then.first, revisedInstruction)
            if (selectedFromOr) {
              then.selectFirstStage(revisedInstruction, reader, loweredBinding)
            } else {
              then.bindFirstStage(revisedInstruction, reader, loweredBinding)
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
      timeline.atomic { doTask(id) }
    } catch (_: AbstractException) {
      explainTask(id, "abstract")
    } catch (_: NotNowException) {
      explainTask(id, "currently impossible")
    }
  }

  internal fun tryTask(
      narrowing: InstructionTree,
      taskNumber: Int? = null,
      intensityOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
  ) {
    val evaluated = evaluatePer(narrowing)
    val id = matchingTask(evaluated, taskNumber, intensityOmitted)
    try {
      doTask(evaluated, taskNumber, intensityOmitted, executeSubmittedGroup)
    } catch (_: AbstractException) {
      explainTask(id, "abstract")
    } catch (_: NotNowException) {
      explainTask(id, "currently impossible")
    }
  }

  // Similar to tryTask, but a NotNowException is unrecoverable once selection holds the lock.
  private fun trySelectedAnyTask(): Boolean /* did I do stuff? */ {
    val taskId = allTasks.selectedTask()!!
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
      addTasks(InstructionGroup.of(parse<InstructionTree>(instruction)), fakeCause)
          .forEach(::doTask)
}
