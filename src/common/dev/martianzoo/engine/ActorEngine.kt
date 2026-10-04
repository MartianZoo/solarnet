package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
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
import dev.martianzoo.state.Component.Companion.toComponent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.Selection
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult

/** Policy-free task and state mutation mechanics attributed to one [actor]. */
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
    private val changer: Changer,
    private val worldTransaction: WorldTransaction,
    private val elaborator: PetElaborator,
) {
  private val allTasks: TaskQueue = gameWorld.tasks

  private object SelectionProbeSucceeded : RuntimeException()

  private object ExecutionProbeSucceeded : RuntimeException()

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

  public fun sneak(changes: InstructionGroup, cause: Cause? = null) {
    changes.instructions.forEach {
      if (it.isAbstract(reader)) {
        throw NotFullySpecifiedException("instruction is abstract: `$it`", it.sourceLocation)
      }
      val change =
          it as? Change
              ?: throw ExpressionException(
                  "sneak accepts only direct changes; found `$it`",
                  sourceLocation = it.sourceLocation,
              )
      val count = change.count as ActualScalar
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

  public fun addTasks(instructions: InstructionGroup, firstCause: Cause? = null): List<TaskId> =
      taskQueues.addTasks(instructions, actor, firstCause).map { it.task.id }

  public fun dropTask(taskId: TaskId): TaskRemovedEvent {
    val task = tasks.getTaskData(taskId)
    return taskQueues.removeTask(task)
  }

  /** Restores prior task data while applying an evidenced replay correction. */
  public fun restoreTask(task: Task) {
    val current = tasks.getTaskData(task.id)
    if (task.assignee != current.assignee) {
      throw TaskException(
          "cannot restore task ${task.id} assigned to `${task.assignee}` over its current " +
              "`${current.assignee}` assignment"
      )
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
          task.actor,
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

  private fun narrowSelectedTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean,
  ) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("`$actor` cannot narrow a task assigned to `${task.assignee}`")
    }

    val effectiveNarrowing = effectiveNarrowing(narrowing, task.instruction, quantifierOmitted)
    if (effectiveNarrowing == task.instruction) {
      selectAndExecuteIfConcrete(tasks, taskId)
      return
    }
    val directlyNarrows = effectiveNarrowing.narrows(task.instruction, reader)
    val selectedThen =
        if (directlyNarrows) null else selectFirstStageOrNull(task.instruction, effectiveNarrowing)
    if (selectedThen == null) effectiveNarrowing.ensureNarrows(task.instruction, reader)

    if (selectedThen != null && task.then != null) {
      throw TaskException("cannot select the first stage of a `THEN` with an outer continuation")
    }
    val continuation = selectedThen?.continuationAfterFirst() ?: task.then

    // A selected group completes structurally before its children resolve against successive
    // worlds.
    val replacement =
        if (effectiveNarrowing is Instruction)
            instructor.resolve(effectiveNarrowing, worldGainNarrowing = true)
        else effectiveNarrowing
    replace1WithN(tasks, task, replacement, then = continuation)
    if (taskId in allTasks) executeSelectedIfConcrete(queueForAnyTask(taskId), taskId)
  }

  @Suppress("TooGenericExceptionCaught") // TODO narrow? log?
  public fun canSelectTask(taskId: TaskId): Boolean {
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
  public fun canExecuteTask(taskId: TaskId): Boolean {
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
    val replacement = instructor.resolve(task.instruction, worldGainNarrowing = true)
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
      val selection =
          if (original.selection == Selection.DELEGATED || instruction.isAbstract(reader)) {
            Selection.DELEGATED
          } else {
            Selection.SELECTED
          }
      taskQueues.editTask(
          original.copy(instruction = instruction, then = then, selection = selection)
      )
    } else {
      // Structural completion replaces the selected task with ordinary pending siblings. No child
      // inherits selection; a later player input must select whichever sibling comes next.
      taskQueues.addTasks(
          group,
          original.controller,
          original.cause,
          original.actor,
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
    newTasks.forEach(taskQueues::addTasks)
    handleTask(queue, selectedTask)
  }

  public fun doTask(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      executeSubmittedGroup: Boolean = false,
      taskId: TaskId? = null,
      contextClass: ClassName? = null,
  ) {
    val evaluated = evaluatePer(narrowing)
    val id = matchingTask(evaluated, taskId, quantifierOmitted, contextClass)
    val tasksBefore = tasks.ids()
    val task = tasks.getTaskData(id)
    val intersection = intersectTask(evaluated, task.instruction, quantifierOmitted)
    if (intersection != null) {
      enforceSelectLock(id)
      narrowSelectedTask(id, intersection, quantifierOmitted)
    } else {
      val selected = selectTask(tasks, task) ?: return
      val instruction = queueForAnyTask(selected).getTaskData(selected).instruction
      narrowTask(
          intersectTask(evaluated, instruction, quantifierOmitted) ?: evaluated,
          quantifierOmitted,
      )
    }
    if (id !in tasks) {
      if (executeSubmittedGroup) {
        tasks.ids().filter { it !in tasksBefore }.forEach(::doTask)
      }
      return
    }
    executeSelectedTask(queueForAnyTask(id), id)
  }

  private fun evaluatePer(instruction: InstructionTree): InstructionTree =
      if (instruction is Per) instructor.resolve(instruction) else instruction

  private fun matchingTask(
      narrowing: InstructionTree,
      taskId: TaskId? = null,
      quantifierOmitted: Boolean = false,
      contextClass: ClassName? = null,
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
      return uniqueMatchingTask(possibleMatches)
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
      timeline.atomic { doTask(id) }
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
      doTask(evaluated, quantifierOmitted, executeSubmittedGroup, taskId)
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
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (_: NotFullySpecifiedException) {
      false
    }
  }

  private fun queueForAnyTask(taskId: TaskId): TaskQueue =
      gameWorld.tasksFor(allTasks.getTaskData(taskId).assignee)
}
