package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
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
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.Selection
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
  // Auto-exec scans the whole game for compatibility with existing workflows. Selection and
  // delegated reassignment are also whole-game concerns, so keep global visibility as a queue view
  // rather than exposing TaskQueues storage.
  private val allTasks = taskQueues.all()

  private object SelectionProbeSucceeded : RuntimeException()

  private object ExecutionProbeSucceeded : RuntimeException()

  private val immutableClassFacts = narrowingFacts(requirementsHold = false)
  private val possibleWorldFacts = narrowingFacts(requirementsHold = true)

  private fun narrowingFacts(requirementsHold: Boolean): TypeInfo =
      object : TypeInfo {
        override fun isAbstract(e: Expression): Boolean = reader.resolve(e).isAbstract(this)

        override fun ensureNarrows(wide: Expression, narrow: Expression) {
          reader.resolve(narrow).ensureNarrows(reader.resolve(wide), this)
        }

        override fun has(requirement: Requirement): Boolean = requirementsHold
      }

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
          actor = actor,
      )
    }
  }

  // TASKS LAYER

  internal fun addTasks(instructions: InstructionGroup, firstCause: Cause? = null): List<TaskId> =
      tasks.addTasks(instructions, firstCause).map { it.task.id }

  internal fun dropTask(taskId: TaskId): TaskRemovedEvent = tasks.removeTask(taskId)

  // OPERATIONS LAYER

  internal fun runOperation(
      initialInstructions: InstructionGroup,
      autoExec: AutoExecPolicy,
      body: () -> Unit,
  ): Set<TaskId> {
    val preexistingTasks = allTasks.ids()
    allTasks.selectedTask()?.let {
      throw TaskException("can't start a manual operation while task $it holds the select-lock")
    }
    addTasks(initialInstructions).forEach(::doInitialTask)
    complete(autoExec, preexistingTasks, body)
    return preexistingTasks
  }

  internal fun beginOperation(
      initialInstructions: InstructionGroup,
      autoExec: AutoExecPolicy,
      body: () -> Unit,
  ) {
    tasks.requireAllQueuesEmpty()
    addTasks(initialInstructions).forEach(::doInitialTask)
    continueOperation(autoExec, body)
  }

  private fun doInitialTask(taskId: TaskId) {
    try {
      doTask(taskId)
    } catch (_: AbstractException) {
      // Initial abstract work remains pending for the operation body to narrow.
    }
  }

  internal fun continueOperation(autoExec: AutoExecPolicy, body: () -> Unit) {
    autoExecNow(autoExec)
    body()
    autoExecNow(autoExec)
  }

  internal fun complete(
      autoExec: AutoExecPolicy,
      allowedPendingTasks: Set<TaskId> = emptySet(),
      body: () -> Unit,
  ) {
    continueOperation(autoExec, body)
    requireComplete(allowedPendingTasks)
  }

  internal fun requireComplete(allowedPendingTasks: Set<TaskId> = emptySet()) {
    val pending = allTasks.extract { it }.filter { it.id !in allowedPendingTasks }
    if (pending.isNotEmpty()) {
      if (pending.any { it.instruction.isAbstract(reader) }) {
        throw AbstractException("pending abstract tasks:\n${pending.joinToString("\n")}")
      }
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
    if (!reader.has(parse("MAX 0 $MUST_CLEAN_UP"))) {
      throw DeadEndException(
          "components requiring cleanup remained after the operation: " +
              reader.getComponents("MustCleanUp").elements
      )
    }
  }

  internal fun autoExecNow(policy: AutoExecPolicy) {
    while (autoExecNext(policy)) {}
  }

  private fun autoExecNext(policy: AutoExecPolicy): Boolean /* should we continue */ {
    if (allTasks.isEmpty()) return false

    // Until Admin has its own scheduled policy, a disabled Player policy still advances
    // deterministic Admin-assigned work without touching any Player task.
    val eligible =
        if (policy == NONE) {
          if (actor !is Player) return false
          allTasks.ids().filter { taskId ->
            queueForAnyTask(taskId).getTaskData(taskId).assignee == ADMIN
          }
        } else {
          allTasks.ids()
        }
    if (eligible.isEmpty()) return false

    val selected = allTasks.selectedTask()
    if (selected != null && selected !in eligible) return false
    val effectivePolicy = if (policy == NONE) EAGER else policy

    val options: List<TaskId> = selected?.let(::listOf) ?: eligible.filter(::canSelectAnyTask)

    when (options.size) {
      0 -> doAnyTask(eligible.first()).also { error("that should've completed") }
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
      else -> if (effectivePolicy == CONCRETE) return false
    }

    // We're using an unsafe policy. Arbitrarily try tasks in stable iteration order.

    var recoverable = false

    for (taskId in options) {
      try {
        timeline.atomic { doAnyTask(taskId) }
        return true
      } catch (_: AbstractException) {
        // we're in trouble if ALL of these are NotNowExceptions
        recoverable = true
      } catch (_: NotNowException) {
        val task = queueForAnyTask(taskId).getTaskData(taskId)
        if (task.instruction.isAbstract(reader)) {
          recoverable = true
        }
      }
    }
    if (!recoverable) throw DeadEndException("")

    return false // presumably everything is abstract
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(queue: TaskQueue, task: Task) {
    task.then?.let {
      queue
          .queueFor(task.controller)
          .addTasks(
              it,
              task.cause,
              task.actor,
              controller = task.controller,
          )
    }
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
    narrowSelectedTask(taskId, narrowing, intensityOmitted)
  }

  internal fun narrowTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      intensityOmitted: Boolean = false,
  ) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("$actor can't narrow a task assigned to ${task.assignee}")
    }
    enforceSelectLock(taskId)
    if (task.selected) {
      narrowSelectedTask(taskId, narrowing, intensityOmitted)
      return
    }

    val effectiveNarrowing =
        effectiveNarrowing(narrowing, task.instruction, intensityOmitted, immutableClassFacts)
    effectiveNarrowing.ensureNarrows(task.instruction, immutableClassFacts)
    if (effectiveNarrowing == task.instruction) return
    val instruction =
        effectiveNarrowing as? Instruction
            ?: throw TaskException("one task can't be narrowed to independent tasks")
    tasks.editTask(task.copy(instructionIn = instruction))
  }

  private fun narrowSelectedTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      intensityOmitted: Boolean,
  ) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("$actor can't narrow a task assigned to ${task.assignee}")
    }

    val effectiveNarrowing = effectiveNarrowing(narrowing, task.instruction, intensityOmitted)
    if (effectiveNarrowing == task.instruction) {
      selectAndExecuteIfConcrete(tasks, taskId)
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

    // A selected group completes structurally before its children resolve against successive
    // worlds.
    val replacement =
        if (effectiveNarrowing is Instruction) instructor.resolve(effectiveNarrowing)
        else effectiveNarrowing
    replace1WithN(tasks, task, replacement, then = continuation)
    if (taskId in allTasks) executeSelectedIfConcrete(queueForAnyTask(taskId), taskId)
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

  @Suppress("TooGenericExceptionCaught") // Keep this probe aligned with canSelectTask for now.
  internal fun canExecuteTask(taskId: TaskId): Boolean {
    return try {
      timeline.atomic {
        doTask(taskId)
        throw ExecutionProbeSucceeded
      }
      false
    } catch (_: ExecutionProbeSucceeded) {
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
    executeSelectedIfConcrete(queueForAnyTask(selected), selected)
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
    replace1WithN(queue, task, replacement, then = task.then)
    return task.id.takeIf { it in allTasks }
  }

  private fun replace1WithN(
      queue: TaskQueue,
      original: Task,
      replacement: InstructionTree,
      then: InstructionGroup?,
  ) {
    val group = InstructionGroup.of(replacement)
    if (group.size == 1) {
      val instruction = group.instructions.single()
      val updated =
          if (instruction is Then && then == null) {
            Task.newTasks(
                    firstId = original.id,
                    controller = original.controller,
                    instruction = group,
                    cause = original.cause,
                    actor = original.actor,
                    isAbstract = reader::isAbstract,
                )
                .single()
          } else {
            original.copy(instructionIn = instruction, thenIn = then)
          }
      val selection =
          if (original.selection == Selection.DELEGATED || instruction.isAbstract(reader)) {
            Selection.DELEGATED
          } else {
            Selection.SELECTED
          }
      allTasks.editTask(updated.copy(selection = selection))
    } else {
      // Structural completion replaces the selected task with ordinary pending siblings. No child
      // inherits selection; a later player input must select whichever sibling comes next.
      queue
          .queueFor(original.controller)
          .addTasks(
              group,
              original.cause,
              original.actor,
              controller = original.controller,
          )
      handleTask(queue, original.copy(thenIn = then))
    }
  }

  internal fun doTask(taskId: TaskId) {
    doTask(tasks, taskId)
  }

  private fun doTask(queue: TaskQueue, taskId: TaskId): Task {
    val original = queue.getTaskData(taskId)
    val selected = selectTask(queue, original) ?: return original
    val selectedQueue = queueForAnyTask(selected)
    val selectedTask = selectedQueue.getTaskData(selected)
    if (selectedTask.instruction.isAbstract(reader)) {
      throw abstractInstruction(selectedTask.instruction)
    }
    executeSelectedTask(selectedQueue, selected)
    return selectedTask
  }

  private fun executeSelectedTask(queue: TaskQueue, taskId: TaskId) {
    val selectedTask = queue.getTaskData(taskId)
    check(selectedTask.selected)
    val newTasks =
        instructor.executeResolved(
            selectedTask.instruction,
            selectedTask.cause,
            selectedTask.actor,
            selectedTask.controller,
        )
    newTasks.forEach { queue.queueFor(it.controller).addTasks(it) }
    handleTask(queue, selectedTask)
  }

  private fun doAnyTask(taskId: TaskId): Task = doTask(queueForAnyTask(taskId), taskId)

  internal fun doTask(
      narrowing: InstructionTree,
      intensityOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
      taskId: TaskId? = null,
  ) {
    val evaluated = evaluatePer(narrowing)
    val id = matchingTask(evaluated, taskId, intensityOmitted)
    val tasksBefore = tasks.ids()
    val task = tasks.getTaskData(id)
    if (narrowsTask(evaluated, task.instruction, intensityOmitted)) {
      enforceSelectLock(id)
      narrowSelectedTask(id, evaluated, intensityOmitted)
    } else {
      selectTask(tasks, task) ?: return
      narrowTask(evaluated, intensityOmitted)
    }
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
      taskId: TaskId? = null,
      intensityOmitted: Boolean = false,
  ): TaskId {
    tasks.selectedTask()?.let { selected ->
      if (taskId != null && taskId != selected) {
        throw TaskException("task $selected is already selected")
      }
      return selected
    }

    if (taskId != null) return tasks.getTaskData(taskId).id

    fun weCanNarrowIt(taskData: Task): Boolean {
      if (taskData.assignee != actor) return false
      val instruction = taskData.instruction
      if (narrowsTask(narrowing, instruction, intensityOmitted)) return true
      if (targetsThenFirstStage(narrowing, instruction, intensityOmitted)) return false
      return try {
        narrowsTask(narrowing, instructor.resolve(instruction), intensityOmitted)
      } catch (_: NotNowException) {
        false
      }
    }

    val assigned = tasks.extract { it }.filter { it.assignee == actor }
    val matches = assigned.filter(::weCanNarrowIt)
    if (matches.isNotEmpty()) return uniqueMatchingTask(matches)

    // A failed live refinement can still identify the intended task. Let normal narrowing report
    // which requirement failed instead of replacing that reason with a generic no-task match.
    val possibleMatches = assigned.filter { task ->
      effectiveNarrowing(narrowing, task.instruction, intensityOmitted, possibleWorldFacts)
          .narrows(task.instruction, possibleWorldFacts)
    }
    return uniqueMatchingTask(possibleMatches)
  }

  private fun targetsThenFirstStage(
      narrowing: InstructionTree,
      existing: InstructionTree,
      intensityOmitted: Boolean,
  ): Boolean {
    val effective = effectiveNarrowing(narrowing, existing, intensityOmitted)
    if (effective !is Instruction || effective is Then) return false
    val candidates =
        when (existing) {
          is Then -> listOf(existing)
          is Or -> existing.instructions.filterIsInstance<Then>()
          else -> emptyList()
        }
    return candidates.any { then ->
      val first = then.first
      val selectable = if (first is Gated) first.inner as Instruction else first
      effective.narrows(selectable, reader)
    }
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
      info: TypeInfo = reader,
  ): InstructionTree {
    if (!intensityOmitted || narrowing !is Change) return narrowing
    if (narrowing.narrows(existing, info)) return narrowing

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
          inheritIntensity(choice).takeIf { inherited -> inherited.narrows(choice, info) }
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
            val loweredBinding = loweredRemovalBinding(then, revisedInstruction)
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

  private fun loweredRemovalBinding(then: Then, narrow: Instruction): PetTransformer? {
    val general = (then.first as? Change)?.removing ?: return null
    val specific = (narrow as? Change)?.removing ?: return null
    val elaborator = (reader as GameReaderImpl).elaborator
    return elaborator.specializeVariables(
        reader.resolve(general),
        reader.resolve(specific),
        general,
        then.typeVariables,
    )
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
      // A probe that needs narrowing leaves the task and event history unchanged.
    } catch (_: NotNowException) {
      // A probe that is unavailable in the current World likewise changes nothing.
    }
  }

  internal fun tryTask(
      narrowing: InstructionTree,
      intensityOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
      taskId: TaskId? = null,
  ) {
    val evaluated = evaluatePer(narrowing)
    try {
      doTask(evaluated, intensityOmitted, executeSubmittedGroup, taskId)
    } catch (_: AbstractException) {
      // A probe that needs narrowing leaves the task and event history unchanged.
    } catch (_: NotNowException) {
      // A probe that is unavailable in the current World likewise changes nothing.
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
      false
    }
  }

  private fun queueForAnyTask(taskId: TaskId): TaskQueue =
      tasks.queueFor(allTasks.getTaskData(taskId).assignee)

  private fun execute(instruction: String, fakeCause: Cause? = null): Unit =
      addTasks(InstructionGroup.of(parse<InstructionTree>(instruction)), fakeCause)
          .forEach(::doTask)
}
