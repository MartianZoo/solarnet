package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
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
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult

/**
 * Policy-free task and state mutation mechanics attributed to [actor].
 *
 * Tasks that gain a `System` component with a fixed scalar or remove a concrete `System` component
 * are assigned to Admin. Player choices remain with the Player until narrowing makes the change
 * concrete. Selection reapplies this rule after resolving enclosing instructions, while explicit
 * instruction-side `BY` remains authoritative. The original controller and selection recipient are
 * retained for continuations and queued effects.
 */
public class ActorEngine
internal constructor(
    /** Tasks currently assigned to [actor]. */
    public val tasks: TaskQueue,
    private val gameWorld: GameWorld,
    private val taskQueues: TaskQueues,
    /** The live game state read by this engine. */
    public val reader: GameReader,
    private val timeline: Timeline,
    /** The Actor to which ordinary mutations through this engine are attributed. */
    public val actor: Actor,
    private val instructor: Instructor,
    private val worldTransaction: WorldTransaction,
    private val elaborator: PetElaborator,
) {
  private val allTasks: TaskQueue = gameWorld.tasks

  private object SelectionProbeSucceeded : RuntimeException()

  private object ExecutionProbeSucceeded : RuntimeException()

  private object ConcreteHandoffValidated : RuntimeException()

  private object SelectionHandoffDetected : RuntimeException()

  private val possibleWorldFacts = narrowingFacts(requirementsHold = true)

  /** Runs one engine transaction and returns its net result. */
  public fun transact(
      settle: () -> Unit,
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult = worldTransaction.run(block, validateCompletion, settle)

  private fun narrowingFacts(requirementsHold: Boolean): TypeInfo =
      object : TypeInfo by reader {
        override fun isAbstract(e: Expression): Boolean = reader.resolve(e).isAbstract(this)

        override fun ensureNarrows(wide: Expression, narrow: Expression) {
          reader.resolve(narrow).ensureNarrows(reader.resolve(wide), this)
        }

        override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
          reader.resolve(narrow).groundType.ensureSelectionNarrows(reader.resolve(wide), this)
        }

        override fun has(requirement: Requirement): Boolean = requirementsHold
      }

  // CHANGES LAYER

  /**
   * Applies and validates a correction with automatic effects, but no queued effects or cleanup.
   */
  public fun sneak(changes: InstructionGroup, cause: Cause? = null): TaskResult =
      worldTransaction.correct {
        instructor.sneak(changes, cause, actor)
      }

  // TASKS LAYER

  public fun addTasks(instructions: InstructionGroup, firstCause: Cause? = null): List<TaskId> =
      taskQueues.addTasks(instructions, actor, firstCause).map { it.task.id }

  public fun dropTask(taskId: TaskId): TaskRemovedEvent {
    val task = tasks.getTaskData(taskId)
    return taskQueues.removeTask(task)
  }

  /** Restores prior task data while applying an evidenced replay correction. */
  public fun restoreTask(task: Task) {
    val current = allTasks.getTaskData(task.id)
    if (actor != current.assignee) {
      throw TaskException("`$actor` cannot restore a task assigned to `${current.assignee}`")
    }
    taskQueues.editTask(task)
  }

  /** Requires that no new pending task or cleanup component remains. */
  public fun requireComplete(allowedPendingTasks: Set<TaskId> = emptySet()) {
    val pending = allTasks.extract { it }.filter { it.id !in allowedPendingTasks }
    if (pending.isNotEmpty()) {
      if (pending.any { it.instruction.isAbstract(reader) }) {
        throw NotFullySpecifiedException("pending abstract tasks:\n${pending.joinToString("\n")}")
      }
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
    if (!reader.has(parse("MAX 0 $MUST_CLEAN_UP"))) {
      throw DeadEndException(
          "components requiring cleanup remained after the operation: " +
              reader.getComponents(MUST_CLEAN_UP.expression).elements
      )
    }
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(queue: TaskQueue, task: Task) {
    task.then?.let {
      taskQueues.addTasks(
          it,
          task.controller,
          task.cause,
          task.selectionAssignee,
      )
    }
    val stored = queue.getTaskData(task.id)
    taskQueues.removeTask(stored)
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

  // TASK COMMANDS

  public fun narrowTask(narrowing: InstructionTree, quantifierOmitted: Boolean = false) {
    val taskId = tasks.selectedTask() ?: throw TaskException("`$actor` has no selected task")
    narrowSelectedTask(taskId, narrowing, quantifierOmitted)
  }

  /** Validates a proposed narrowing without changing the task or selecting it. */
  public fun prepareTaskNarrowing(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
  ): InstructionTree =
      prepareNarrowing(taskId, narrowing, quantifierOmitted).let {
        it.selectedThen ?: it.effective
      }

  /** Current executable capacity for a concrete change; null means a missing destination. */
  public fun changeLimit(change: Change): Int? = instructor.changeLimit(change)

  private data class PreparedNarrowing(
      val effective: InstructionTree,
      val selectedThen: Then?,
  )

  private fun prepareNarrowing(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean,
  ): PreparedNarrowing {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("`$actor` cannot narrow a task assigned to `${task.assignee}`")
    }
    val effective = effectiveNarrowing(narrowing, task.instruction, quantifierOmitted)
    if (effective.narrows(task.instruction, reader)) return PreparedNarrowing(effective, null)
    val selectedThen = selectFirstStageOrNull(task.instruction, effective)
    if (selectedThen != null) {
      if (task.then != null) {
        throw TaskException("cannot select the first stage of a `THEN` with an outer continuation")
      }
      return PreparedNarrowing(effective, selectedThen)
    }
    effective.ensureNarrows(task.instruction, reader)
    return PreparedNarrowing(effective, null)
  }

  /** Commits a narrowing of this Actor's task, selecting it if necessary. */
  public fun narrowTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
  ) {
    enforceSelectLock(taskId)
    val task = tasks.getTaskData(taskId)
    var effectiveNarrowing = narrowing
    if (!task.selected) {
      val prepared = prepareNarrowing(taskId, narrowing, quantifierOmitted)
      val narrowsBeforeHandoff =
          prepared.selectedThen != null || prepared.effective != task.instruction
      val contextualAssignee = taskQueues.assigneeAfterContextualSelection(task)
      if (contextualAssignee != actor && narrowsBeforeHandoff) {
        throw TaskException(
            "`$actor` cannot narrow task $taskId because selection assigns it to " +
                "`$contextualAssignee`"
        )
      }
      val selected = selectTask(tasks, task) ?: return
      val selectedTask = allTasks.getTaskData(selected)
      if (selectedTask.assignee != actor) {
        if (narrowsBeforeHandoff) {
          throw TaskException(
              "`$actor` cannot narrow task $taskId because selection assigns it to " +
                  "`${selectedTask.assignee}`"
          )
        }
        return
      }
      if (
          !narrowing.isAbstract(reader) &&
              narrowing is By &&
              instructor.actorFor(narrowing) == actor
      ) {
        effectiveNarrowing = narrowing.inner
      }
    }
    narrowSelectedTask(taskId, effectiveNarrowing, quantifierOmitted)
  }

  private fun narrowSelectedTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean,
  ) {
    val task = tasks.getTaskData(taskId)
    val prepared = prepareNarrowing(taskId, narrowing, quantifierOmitted)
    val effectiveNarrowing = prepared.effective
    if (effectiveNarrowing == task.instruction) {
      selectAndExecuteIfConcrete(tasks, taskId)
      return
    }
    val continuation = prepared.selectedThen?.continuationAfterFirst() ?: task.then

    // A selected group completes structurally before its children resolve against successive
    // worlds.
    val replacement =
        if (effectiveNarrowing is Instruction)
            instructor.resolve(effectiveNarrowing, worldGainNarrowing = true)
        else effectiveNarrowing
    replace1WithN(tasks, task, replacement, then = continuation)
    if (taskId in allTasks) executeSelectedIfConcrete(taskId)
  }

  @Suppress("TooGenericExceptionCaught") // TODO narrow? log?
  public fun canSelectTask(taskId: TaskId): Boolean {
    return try {
      timeline.atomic {
        val selected = selectTask(tasks, tasks.getTaskData(taskId))
        if (selected != null) {
          val selectedQueue = queueForAnyTask(selected)
          val selectedTask = selectedQueue.getTaskData(selected)
          if (!selectedTask.instruction.isAbstract(reader)) {
            executeSelectedTask(selectedQueue, selected, selectedTask.assignee)
          }
        }
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
  public fun canExecuteTask(taskId: TaskId): Boolean {
    return try {
      timeline.atomic {
        doTask(taskId)
        if (taskId !in allTasks) throw ExecutionProbeSucceeded
        throw AbortTransactionException()
      }
      false
    } catch (_: ExecutionProbeSucceeded) {
      true
    } catch (_: Exception) {
      false
    }
  }

  public fun selectTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("`$actor` cannot select a task assigned to `${task.assignee}`")
    }
    selectAndExecuteIfConcrete(tasks, taskId)
  }

  public fun selectTask(instruction: Instruction): Unit =
      selectTask(taskWithInstruction(instruction))

  private fun selectAndExecuteIfConcrete(queue: TaskQueue, taskId: TaskId) {
    val selected = selectTask(queue, queue.getTaskData(taskId)) ?: return
    executeSelectedIfConcrete(selected)
  }

  private fun executeSelectedIfConcrete(taskId: TaskId) {
    val task = allTasks.getTaskData(taskId)
    if (task.assignee == actor && !task.instruction.isAbstract(reader)) {
      executeSelectedTask(tasks, taskId)
    }
  }

  private fun selectTask(queue: TaskQueue, task: Task): TaskId? {
    enforceSelectLock(task.id)
    if (task.selected) return task.id
    val replacement = instructor.resolve(task.instruction, worldGainNarrowing = true)
    replace1WithN(queue, task, replacement, then = task.then)
    return task.id.takeIf { it in allTasks }
  }

  private fun selectionHandsOff(queue: TaskQueue, task: Task): Boolean {
    return try {
      timeline.atomic {
        val selected = selectTask(queue, task) ?: throw AbortTransactionException()
        if (allTasks.getTaskData(selected).assignee != actor) throw SelectionHandoffDetected
        throw AbortTransactionException()
      }
      false
    } catch (_: SelectionHandoffDetected) {
      true
    }
  }

  private fun replace1WithN(
      queue: TaskQueue,
      original: Task,
      replacement: InstructionTree,
      then: InstructionGroup?,
  ) {
    val group = InstructionGroup.of(replacement)
    if (group.size == 1) {
      var updated =
          taskQueues.normalizeForSelection(
              original.copy(
                  assignee =
                      if (original.selected) original.assignee else original.selectionAssignee,
                  selected = true,
                  instruction = group.instructions.single(),
                  then = then,
              )
          )
      val instruction = updated.instruction
      if (!instruction.isAbstract(reader) && instruction is By) {
        updated =
            updated.copy(
                assignee = instructor.actorFor(instruction),
                instruction = instruction.inner,
            )
      }
      taskQueues.editTask(updated)
      if (updated.assignee != actor && !updated.instruction.isAbstract(reader)) {
        validateConcreteHandoff(updated.id)
      }
    } else {
      // Structural completion replaces the selected task with ordinary pending siblings. No child
      // inherits selection; a later player input must select whichever sibling comes next.
      taskQueues.addTasks(
          group,
          original.controller,
          original.cause,
          original.selectionAssignee,
      )
      handleTask(queue, original.copy(then = then))
    }
  }

  public fun doTask(taskId: TaskId) {
    doTask(tasks, taskId)
  }

  private fun doTask(queue: TaskQueue, taskId: TaskId): Task {
    val original = queue.getTaskData(taskId)
    val selected = selectTask(queue, original) ?: return original
    val selectedQueue = queueForAnyTask(selected)
    val selectedTask = selectedQueue.getTaskData(selected)
    if (selectedTask.assignee != actor) return selectedTask
    executeSelectedTask(selectedQueue, selected)
    return selectedTask
  }

  private fun executeSelectedTask(
      queue: TaskQueue,
      taskId: TaskId,
      executingActor: Actor = actor,
  ) {
    val selectedTask = queue.getTaskData(taskId)
    check(selectedTask.selected)
    check(selectedTask.assignee == executingActor)
    val newTasks =
        instructor.executeResolved(
            selectedTask.instruction,
            selectedTask.cause,
            executingActor,
            selectedTask.controller,
            selectedTask.selectionAssignee,
        )
    newTasks.forEach(taskQueues::addTasks)
    handleTask(queue, selectedTask)
  }

  private fun validateConcreteHandoff(taskId: TaskId) {
    try {
      timeline.atomic {
        val task = allTasks.getTaskData(taskId)
        executeSelectedTask(queueForAnyTask(taskId), taskId, task.assignee)
        throw ConcreteHandoffValidated
      }
    } catch (_: ConcreteHandoffValidated) {
      // The target Actor can execute it; the nested transaction restored the handed-off task.
    }
  }

  /**
   * Executes a matching task. With [combineScalars], a missing match can instead execute every
   * concrete scalar-only match when their total equals the submitted change. Existing matches,
   * including ambiguous matches, take precedence; newly caused tasks do not join that total.
   */
  public fun doTask(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
      taskId: TaskId? = null,
      contextClass: ClassName? = null,
      combineScalars: Boolean = false,
  ) {
    val evaluated = evaluatePer(narrowing)
    val id = matchingTask(evaluated, taskId, quantifierOmitted, contextClass)
    if (id == null) {
      if (combineScalars && executeScalarSum(evaluated, quantifierOmitted, contextClass)) return
      throw TaskException(
          "no matching task; available tasks: ${if (tasks.isEmpty()) "none" else "\n$tasks"}"
      )
    }
    val tasksBefore = tasks.ids()
    val task = tasks.getTaskData(id)
    if (!task.selected && selectionHandsOff(tasks, task)) {
      throw TaskException(
          "`$actor` cannot do task $id because selection hands it to another Actor; use selectTask"
      )
    }
    val intersection = intersectTask(evaluated, task.instruction, quantifierOmitted)
    if (intersection != null) {
      enforceSelectLock(id)
      narrowSelectedTask(id, intersection, quantifierOmitted)
    } else {
      val selected = selectTask(tasks, task) ?: return
      val selectedTask = queueForAnyTask(selected).getTaskData(selected)
      if (selectedTask.assignee != actor) return
      val instruction = selectedTask.instruction
      narrowTask(
          intersectTask(evaluated, instruction, quantifierOmitted) ?: evaluated,
          quantifierOmitted,
      )
    }
    if (id !in tasks) {
      if (executeSubmittedGroup) {
        for (newTask in tasks.ids().filter { it !in tasksBefore }) {
          if (allTasks.selectedTask() != null) break
          doTask(newTask)
        }
      }
      return
    }
    val selectedTask = allTasks.getTaskData(id)
    if (selectedTask.assignee == actor) executeSelectedTask(tasks, id)
  }

  private fun evaluatePer(instruction: InstructionTree): InstructionTree =
      if (instruction is Per) instructor.resolve(instruction) else instruction

  private fun executeScalarSum(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean,
      contextClass: ClassName?,
  ): Boolean {
    // Atomized gains arrive as a group, but still describe a single submitted total.
    val changes = InstructionGroup.of(narrowing).instructions.map { it as? Change ?: return false }
    val first = changes.firstOrNull() ?: return false
    if (changes.any { it.isAbstract(reader) }) return false
    fun sameChange(change: Change): Boolean =
        change.gaining == first.gaining &&
            change.removing == first.removing &&
            (quantifierOmitted || change.quantifier == first.quantifier)
    if (changes.any { !sameChange(it) }) return false
    val total = changes.sumOf { (it.count as ActualScalar).value.toLong() }
    val matches =
        tasks
            .extract { it }
            .filter { contextClass == null || it.cause?.context?.className == contextClass }
            .mapNotNull { task ->
              val resolved =
                  try {
                    instructor.resolve(task.instruction)
                  } catch (_: NotNowException) {
                    return@mapNotNull null
                  }
              (resolved as? Change)?.takeIf(::sameChange)?.let { task.copy(instruction = it) }
            }
    if (matches.size < 2) return false
    if (matches.map { (it.instruction as Change).quantifier }.distinct().size != 1) return false
    val amounts = matches.map {
      (it.instruction as Change).count as? ActualScalar ?: return false
    }
    if (amounts.sumOf { it.value.toLong() } != total) return false
    matches.forEach { doTask(it.instruction, taskId = it.id) }
    return true
  }

  private fun matchingTask(
      narrowing: InstructionTree,
      taskId: TaskId? = null,
      quantifierOmitted: Boolean = false,
      contextClass: ClassName? = null,
  ): TaskId? {
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
      if (intersectTask(narrowing, instruction, quantifierOmitted) != null) return true
      if (targetsThenFirstStage(narrowing, instruction, quantifierOmitted)) return false
      return try {
        intersectTask(narrowing, instructor.resolve(instruction), quantifierOmitted) != null
      } catch (_: NotNowException) {
        false
      }
    }

    val assigned =
        tasks
            .extract { it }
            .filter {
              it.assignee == actor &&
                  (contextClass == null || it.cause?.context?.className == contextClass)
            }
    try {
      val matches = assigned.filter(::weCanNarrowIt)
      if (matches.isNotEmpty()) return uniqueMatchingTask(matches)

      // A failed live refinement can still identify the intended task. Let normal narrowing report
      // which requirement failed instead of replacing that reason with a generic no-task match.
      val possibleMatches = assigned.filter { task ->
        intersectTask(narrowing, task.instruction, quantifierOmitted, possibleWorldFacts) != null
      }
      return if (possibleMatches.isEmpty()) null else uniqueMatchingTask(possibleMatches)
    } catch (e: NarrowingException) {
      throw TaskException("cannot identify a task for `$narrowing`: ${e.message}", e)
    }
  }

  private fun targetsThenFirstStage(
      narrowing: InstructionTree,
      existing: InstructionTree,
      quantifierOmitted: Boolean,
  ): Boolean {
    val effective = effectiveNarrowing(narrowing, existing, quantifierOmitted)
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

  private fun intersectTask(
      narrowing: InstructionTree,
      existing: InstructionTree,
      quantifierOmitted: Boolean,
      info: TypeInfo = reader,
  ): InstructionTree? {
    val effective =
        effectiveNarrowing(narrowing, existing, quantifierOmitted, info, intersect = true)
    effective.intersect(existing, reader.classTable, info)?.let {
      return it
    }
    if (selectFirstStageOrNull(existing, effective) != null) return effective
    val sequences =
        when (existing) {
          is Then -> listOf(existing)
          is Or -> existing.instructions.filterIsInstance<Then>()
          else -> emptyList()
        }
    return sequences
        .mapNotNull { then ->
          val first = then.first
          val selectable = if (first is Gated) first.inner else first
          val head =
              effectiveNarrowing(narrowing, selectable, quantifierOmitted, info, intersect = true)
                  .intersect(selectable, reader.classTable, info)
          head?.takeIf { selectFirstStageOrNull(existing, it) != null }
        }
        .distinct()
        .singleOrNull()
  }

  private fun effectiveNarrowing(
      narrowing: InstructionTree,
      existing: InstructionTree,
      quantifierOmitted: Boolean,
      info: TypeInfo = reader,
      intersect: Boolean = false,
  ): InstructionTree {
    if (!quantifierOmitted || narrowing !is Change) return narrowing
    fun matches(proposed: InstructionTree, instruction: InstructionTree): Boolean =
        if (intersect) proposed.intersect(instruction, reader.classTable, info) != null
        else proposed.narrows(instruction, info)
    if (matches(narrowing, existing)) return narrowing

    fun inheritQuantifier(change: Change): InstructionTree =
        when (narrowing) {
          is Gain -> Gain.gain(narrowing.scaledEx, change.quantifier)
          is Remove -> Remove.remove(narrowing.scaledEx, change.quantifier)
          is Transmute -> narrowing.copy(quantifier = change.quantifier)
        }

    val choices =
        when (existing) {
          is Change -> listOf(existing)
          is Or -> existing.instructions.filterIsInstance<Change>()
          else -> emptyList()
        }
    return choices
        .mapNotNull { choice ->
          // Inheritance repairs a quantifier mismatch, not incompatible Types or counts. In
          // particular, do not turn an unrelated mandatory request into an optional Ok match.
          if (intersect && narrowing.quantifier!!.narrows(choice.quantifier!!, info)) {
            return@mapNotNull null
          }
          inheritQuantifier(choice).takeIf { inherited -> matches(inherited, choice) }
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
    val candidates: List<Then> =
        when (instruction) {
          is Then -> listOf(instruction)
          is Or -> instruction.instructions.filterIsInstance<Then>()
          else -> emptyList()
        }
    val firstStageInfo =
        object : TypeInfo by reader {
          override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
            val type = reader.resolve(narrow)
            val selected = reader.classTable.singleConcreteSubtype(type, reader) ?: type
            selected.groundType.ensureSelectionNarrows(reader.resolve(wide), this)
          }
        }
    return candidates
        .mapNotNull { then ->
          try {
            val loweredBinding = loweredTypeBinding(then, revisedInstruction)
            then.selectFirstStage(revisedInstruction, firstStageInfo, loweredBinding)
          } catch (_: NarrowingException) {
            null
          }
        }
        .singleOrNull()
  }

  private fun loweredTypeBinding(then: Then, narrow: Instruction): PetTransformer? {
    val general = then.first as? Change ?: return null
    val specific = narrow as? Change ?: return null
    return PetTransformer.chain(
        listOf(general.gaining to specific.gaining, general.removing to specific.removing)
            .mapNotNull { (authored, proposed) ->
              if (authored == null || proposed == null) return@mapNotNull null
              val proposedType = reader.resolve(proposed)
              val selected =
                  reader.classTable.singleConcreteSubtype(proposedType, reader) ?: proposedType
              elaborator.specializeVariables(
                  reader.resolve(authored),
                  selected,
                  authored,
                  then.typeVariables,
              )
            }
    )
  }

  private fun taskWithInstruction(instruction: Instruction): TaskId =
      uniqueMatchingTask(tasks.extract { it }.filter { it.instruction == instruction })

  private fun uniqueMatchingTask(matches: List<Task>): TaskId {
    val first =
        matches.firstOrNull()
            ?: throw TaskException(
                "no matching task; available tasks: ${if (tasks.isEmpty()) "none" else "\n$tasks"}"
            )
    // Origin metadata does not distinguish choices that otherwise present and behave identically.
    if (matches.map { it.copy(id = first.id, cause = first.cause) }.distinct().size == 1) {
      return first.id
    }
    throw TaskException(
        "${matches.size} matching tasks are ambiguous; select one by its task ID:\n${matches.joinToString("\n")}"
    )
  }

  /** Tries [id], leaving it pending when it needs a choice or is unavailable. */
  public fun tryTask(id: TaskId) {
    try {
      timeline.atomic {
        doTask(id)
        if (id in allTasks) throw AbortTransactionException()
      }
    } catch (_: NotFullySpecifiedException) {
      // A probe that needs narrowing leaves the task and event history unchanged.
    } catch (_: NotNowException) {
      // A probe that is unavailable in the current World likewise changes nothing.
    }
  }

  public fun tryTask(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
      taskId: TaskId? = null,
  ) {
    val evaluated = evaluatePer(narrowing)
    try {
      timeline.atomic {
        doTask(evaluated, quantifierOmitted, executeSubmittedGroup, taskId)
        if (allTasks.selectedTask() != null) throw AbortTransactionException()
      }
    } catch (_: NotFullySpecifiedException) {
      // A probe that needs narrowing leaves the task and event history unchanged.
    } catch (_: NotNowException) {
      // A probe that is unavailable in the current World likewise changes nothing.
    }
  }

  // Similar to tryTask, but a NotNowException is unrecoverable once selection holds the lock.
  public fun trySelectedTask(): Boolean {
    val taskId = allTasks.selectedTask()!!
    return try {
      doTask(queueForAnyTask(taskId), taskId)
      taskId !in allTasks
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (_: NotFullySpecifiedException) {
      false
    }
  }

  private fun queueForAnyTask(taskId: TaskId): TaskQueue =
      gameWorld.tasksFor(allTasks.getTaskData(taskId).assignee)
}
