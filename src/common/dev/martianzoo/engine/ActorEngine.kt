package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.ResourceLimitException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Deferral.FINALLY
import dev.martianzoo.pets.ast.Deferral.NONE
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
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
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult

/**
 * Executes task and correction commands attributed to [actor] in a shared game world.
 *
 * A task command applies its requested change, dependent removals, recursive `::` effects, and
 * invariant checks. Resulting tasks remain queued for their assigned Actors. Concrete System
 * changes are assigned to Admin; Player choices remain with the Player until the change is
 * concrete. At selection, a concrete instruction with explicit `BY` goes to the named Actor. The
 * engine calculates consequences; the clients decide which tasks to submit.
 *
 * For example, placing greenery on a Land Claim cube removes the cube through `::` before
 * returning; the placement bonus remains queued for Admin.
 *
 * Methods may rely on client-controlled rollback instead of restoring their own failures. Unless a
 * method promises failure-atomicity, the client must arrange restoration before continuing
 * gameplay; [Timeline.atomic] supplies a restoring block for ordinary game-rule failures.
 *
 * The proposed contract excludes Player selection while ordinary Admin work remains. That check is
 * currently missing. Persistent partial selection also remains under review.
 *
 * [transact] groups commands and caller-supplied task handling in a rollback scope. That block may
 * include several transactions under the proposed contract; its current behavior is described
 * below.
 */
public class ActorEngine
internal constructor(
    /**
     * Live filtered view of tasks assigned to [actor]; another Actor's selection is not in this
     * view.
     */
    public val tasks: TaskQueue,
    private val gameWorld: GameWorld,
    private val taskQueues: TaskQueues,
    /**
     * Live state and type queries for the same world as this engine, including other Actors'
     * components.
     */
    public val reader: GameReader,
    private val timeline: TimelineImpl,
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

  /**
   * Runs [block] and subsequent task handling, returning their combined [TaskResult]. The intended
   * synchronous behavior is to run to quiescence and report all changes made before return.
   *
   * Currently runs [afterChanges] and [validateCompletion] for the outermost call. Ordinary nested
   * calls skip their own finishing steps; a call made directly by the completion callback runs its
   * own finishing steps. Agent supplies task handling and application validation, or validation
   * alone.
   *
   * After the outermost restoring block returns normally, including cancellation through
   * [AbortTransactionException], invokes [World.onTransactionComplete]. If the World is still at
   * the end of its history afterward, runs [afterChanges] again. This wrapper does not assign an
   * Actor to changes; the commands called inside it do that.
   *
   * Current limitation: the result contains only component changes and surviving new tasks from
   * [block] and the first finishing steps. It omits callback changes and subsequent task handling,
   * contrary to the intended result coverage. Failure in that later work does not restore the
   * initial work.
   *
   * The proposed contract requires eligible Admin work to finish before return and recommends
   * restoring the whole call on failure wherever practical, subject to human review.
   */
  // TODO(ENGINE_CONTRACT): Review whether this block-based coordinator belongs in the public API.
  // Preserve synchronous execution through quiescence and complete TaskResult reporting.
  public fun transact(
      afterChanges: () -> Unit,
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult = worldTransaction.run(block, validateCompletion, afterChanges)

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
   * Applies [changes] as a failure-atomic correction attributed to [actor], optionally using
   * [cause] for attribution. Recursive `::` effects and final invariant checks still run; queued
   * `:` effects and client task handling do not. Returns the correction's component changes and
   * surviving new tasks.
   *
   * This bypasses ordinary task selection and does not call the world's completion callback. The
   * proposed treatment of corrections while a task is selected or Admin work remains needs review.
   */
  public fun sneak(changes: InstructionGroup, cause: Cause? = null): TaskResult =
      worldTransaction.correct {
        instructor.sneak(changes, cause, actor)
      }

  // TASKS LAYER

  /**
   * Adds [instructions] as work initially assigned to [actor], recording [firstCause] when
   * supplied. This attributes the initial work; triggered effects record their own causes. Returns
   * the ids of tasks actually added in creation order. Normalization can discard no-ops, split
   * groups, perform dispatches, and assign concrete System changes to Admin.
   *
   * Adding work does not execute its ordinary task bodies. Creating tasks can promote deferred work
   * before the caller's transaction finishes. A later failure can leave earlier additions behind
   * unless an enclosing [Timeline.atomic] block, such as [transact]'s, restores them.
   */
  // TODO(ENGINE_CONTRACT): Review whether direct queue editing belongs in the public API.
  public fun addTasks(instructions: InstructionGroup, firstCause: Cause? = null): List<TaskId> =
      taskQueues.addTasks(instructions, actor, firstCause).map { it.task.id }

  /**
   * Removes an existing task assigned to [actor] and returns its removal event. Does not execute
   * the removed task's instruction. Rejects a missing task or a task belonging to another Actor.
   *
   * Current limitation: removing work may promote deferred tasks and perform their dispatches. It
   * does not enforce selected-task exclusion or handle newly eligible Admin work by itself.
   */
  // TODO(ENGINE_CONTRACT): Review removing direct task deletion from the engine's public API.
  public fun dropTask(taskId: TaskId): TaskRemovedEvent {
    val task = tasks.getTaskData(taskId)
    return taskQueues.removeTask(task)
  }

  /**
   * Replaces the stored data of an existing task during an evidenced correction. [task]'s id must
   * already exist and its current assignee must be [actor]; this does not recreate a removed task.
   *
   * Normalization applies to the replacement. Current limitation: replacement can change
   * instruction, selection, and assignment without ordinary selection checks. Its place in the
   * public API needs review.
   */
  // TODO(ENGINE_CONTRACT): Investigate limiting this arbitrary task replacement to correction
  // clients.
  public fun restoreTask(task: Task) {
    val current = allTasks.getTaskData(task.id)
    if (actor != current.assignee) {
      throw TaskException("`$actor` cannot restore a task assigned to `${current.assignee}`")
    }
    taskQueues.editTask(task)
  }

  /**
   * Checks completion of the operation's work without executing any pending task.
   *
   * Throws if a pending task outside [allowedPendingTasks], or a `MustCleanUp` component, remains.
   * If tasks remain but `FINALLY` work has been promoted since [lastDecision], those tasks are
   * accepted and cleanup is counted as of just before that promotion. With no relevant pending
   * tasks, cleanup is checked in the current state.
   *
   * This is an operation-level check, not a transaction's final invariant check. Using `FINALLY` as
   * proof of completion is retained behavior whose role needs review.
   */
  // TODO(ENGINE_CONTRACT): Review this operation completion check and its public placement.
  public fun requireComplete(
      allowedPendingTasks: Set<TaskId> = emptySet(),
      lastDecision: Checkpoint? = null,
  ) {
    val pending = allTasks.extract { it }.filter { it.id !in allowedPendingTasks }
    val workOver = lastDecision?.let(::finalWorkBegan)
    if (pending.isNotEmpty() && workOver == null) {
      if (pending.any { it.instruction.isAbstract(reader) }) {
        throw NotFullySpecifiedException("pending abstract tasks:\n${pending.joinToString("\n")}")
      }
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
    val cleanupLeft =
        if (pending.isEmpty() || workOver == null) !reader.has(parse("MAX 0 $MUST_CLEAN_UP"))
        else cleanupComponentsAt(workOver) > 0
    if (cleanupLeft) {
      throw DeadEndException(
          "components requiring cleanup remained after the operation: " +
              reader.getComponents(MUST_CLEAN_UP.expression).elements
      )
    }
  }

  /** Where the World's `FINALLY` work first began since [checkpoint], if it has. */
  private fun finalWorkBegan(checkpoint: Checkpoint): Checkpoint? =
      gameWorld.events
          .entriesSince(checkpoint)
          .firstOrNull {
            it is TaskEditedEvent && it.oldTask.deferral == FINALLY && it.task.deferral == NONE
          }
          ?.let { Checkpoint(it.ordinal) }

  /** How many cleanup components existed at [checkpoint]: those present now, less later change. */
  private fun cleanupComponentsAt(checkpoint: Checkpoint): Int {
    val mustCleanUp = reader.classTable.getClass(MUST_CLEAN_UP)
    fun cleanup(component: Component?) =
        component?.type?.rootClass?.isSubtypeOf(mustCleanUp) == true
    val changeSince =
        gameWorld.events.changesSince(checkpoint).sumOf {
          (if (cleanup(it.change.gaining)) it.change.count else 0) -
              (if (cleanup(it.change.removing)) it.change.count else 0)
        }
    return reader.count(reader.resolve(MUST_CLEAN_UP.expression)) - changeSince
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(queue: TaskQueue, task: Task) {
    task.then?.let {
      taskQueues.addTasks(
          it,
          task.assignee,
          task.cause,
      )
    }
    val stored = queue.getTaskData(task.id)
    taskQueues.removeTask(stored)
  }

  private fun requireNoOtherSelectedTask(taskId: TaskId) {
    // Check the whole task pool: another Actor's selection also excludes this task command.
    // TODO(ENGINE_CONTRACT): Queue edits and corrections currently bypass this exclusion.
    val already = allTasks.selectedTask()
    if (already != null && already != taskId) {
      val instr = allTasks.getTaskData(already).instruction
      throw TaskException("task $already ($instr) is selected and must finish first")
    }
  }

  // TASK COMMANDS

  /**
   * Supplies [narrowing] for this Actor's selected task. When [quantifierOmitted] is true, retains
   * the pending task's quantifier where needed. Resolves the resulting instruction and executes it
   * when concrete, or leaves it assigned to the Actor that must handle it next, as [selectTask]
   * does.
   *
   * Rejects a missing selection or incompatible narrowing. A rejected call can leave changes unless
   * an enclosing [Timeline.atomic] block restores them. It can also store a partial choice; keeping
   * such choices in the client is a proposal awaiting review.
   */
  public fun narrowTask(narrowing: InstructionTree, quantifierOmitted: Boolean = false) {
    val taskId = tasks.selectedTask() ?: throw TaskException("`$actor` has no selected task")
    narrowSelectedTask(taskId, narrowing, quantifierOmitted)
  }

  /**
   * Returns the instruction that submitting [narrowing] for [taskId] would use, without selecting
   * or changing the task. [quantifierOmitted] allows the pending quantifier to fill an omitted
   * amount rule. The task must belong to [actor]; an incompatible narrowing throws.
   *
   * This checks instruction compatibility, including a chosen first `THEN` stage. It does not
   * execute effects or final invariant checks and therefore does not establish legal narrowing
   * under the proposed whole-transaction definition.
   */
  // TODO(ENGINE_CONTRACT): Investigate making this partial-choice preparation an internal client
  // detail.
  public fun prepareTaskNarrowing(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
  ): InstructionTree =
      prepareNarrowing(taskId, narrowing, quantifierOmitted).let {
        it.selectedThen ?: it.effective
      }

  /**
   * Returns the immediate quantity bound for a change with concrete gain/removal targets, or null
   * if a required destination dependency is absent. The change's quantifier determines whether
   * count invariants contribute to the bound; mandatory changes do not use them to hide failing
   * choices.
   *
   * This reads current state without executing the change. Its result does not establish that the
   * change's `::` consequences and final checks can complete successfully.
   */
  // TODO(ENGINE_CONTRACT): Review exposing this incomplete capacity check outside the engine.
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

  /**
   * Selects this Actor's [taskId] as [selectTask] does and supplies [narrowing], retaining an
   * omitted quantifier when [quantifierOmitted] is true. Resolves and executes a concrete result,
   * or leaves the task pending for its current assignee.
   *
   * Rejects a competing selection, deferred work, an incompatible narrowing, or narrowing that
   * belongs to a different Actor after selection. A structural task may split into independent
   * child tasks. Failure restoration needs an enclosing [Timeline.atomic] block. Persistent partial
   * choices remain under review.
   */
  public fun narrowTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
  ) {
    requireNoOtherSelectedTask(taskId)
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
      val selected =
          selectTask(tasks, task)
              ?: if (narrowsBeforeHandoff) {
                throw TaskException(
                    "task $taskId splits during selection; select it before narrowing its child tasks"
                )
              } else return
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

  // TODO(ENGINE_CONTRACT): Review storing partial choices instead of keeping unfinished forms in
  // clients.
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
    val resolved =
        if (effectiveNarrowing is Instruction)
            instructor.resolve(effectiveNarrowing, worldGainNarrowing = true)
        else effectiveNarrowing
    applyResolution(tasks, task, resolved, then = continuation)
    if (taskId in allTasks) executeSelectedIfConcrete(taskId)
  }

  /**
   * Checks whether this task can be selected against the current world, restoring attempted changes
   * before returning. A concrete selection also attempts execution; an abstract selection can
   * return true while still requiring choices. This does not handle later queued tasks.
   *
   * This is not a count of legal concrete narrowings. Current limitations: the attempt can notify
   * live observers, and most exceptions become false, including unexpected implementation errors.
   * [ResourceLimitException] propagates.
   */
  // TODO(ENGINE_CONTRACT): Investigate removing this public probe; do not hide implementation
  // faults.
  @Suppress("TooGenericExceptionCaught")
  public fun canSelectTask(taskId: TaskId): Boolean {
    timeline.requireAtEnd()
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
    } catch (e: ResourceLimitException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Checks whether this Actor can select and finish [taskId] now, restoring attempted changes
   * before returning. Returns false if further narrowing or a different Actor is needed. Newly
   * queued work is outside this check.
   *
   * Current limitations: live observers can see attempted changes, and most exceptions become
   * false, including unexpected implementation errors. [ResourceLimitException] propagates. This
   * API's role in determining proposed transaction-level legality needs review.
   */
  // TODO(ENGINE_CONTRACT): Review this probe against complete-transaction legality and observer
  // effects.
  @Suppress("TooGenericExceptionCaught")
  public fun canExecuteTask(taskId: TaskId): Boolean {
    timeline.requireAtEnd()
    return try {
      timeline.atomic {
        doTask(taskId)
        if (taskId !in allTasks) throw ExecutionProbeSucceeded
        throw AbortTransactionException()
      }
      false
    } catch (_: ExecutionProbeSucceeded) {
      true
    } catch (e: ResourceLimitException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Selects this Actor's [taskId], resolves its instruction against current state, and executes it
   * if concrete and still assigned to this Actor. Otherwise the selected task remains pending for
   * its current assignee; independent structural children are admitted without inheriting
   * selection.
   *
   * Current limitation: when resolution makes the task concrete and reassigns it, this call
   * executes the receiving Actor's instruction temporarily and restores that attempt. Failure
   * rejects the selection, and live observers can see the attempted changes. This examines work
   * that the proposal would assign to the receiving Actor's subsequent transaction.
   *
   * Rejects deferred work and a different selected task. The proposed contract also excludes Player
   * selection while ordinary Admin work remains; that check is currently missing. Failure
   * restoration currently needs an enclosing [Timeline.atomic] block.
   */
  public fun selectTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("`$actor` cannot select a task assigned to `${task.assignee}`")
    }
    selectAndExecuteIfConcrete(tasks, taskId)
  }

  /**
   * Selects a task compatible with [narrowing], restricted to [taskId] when supplied, and applies
   * the narrowing. [quantifierOmitted] lets an omitted quantifier come from the pending task.
   * Distinct matching tasks are rejected as ambiguous; identical tasks differing only in identity
   * and cause currently use the first match.
   *
   * Resolves and executes concrete work as [selectTask] does. Failure restoration needs an
   * enclosing [Timeline.atomic] block. Current differences from the proposal: partial selection can
   * persist, and ordinary Admin work does not block Player selection.
   */
  public fun selectTask(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      taskId: TaskId? = null,
  ) {
    val evaluated = evaluatePer(narrowing)
    val id =
        (if (taskId == null && tasks.selectedTask() == null) {
          val exact =
              tasks.extract { it }.filter { it.assignee == actor && it.instruction == narrowing }
          if (exact.isNotEmpty()) uniqueMatchingTask(exact)
          else matchingTask(evaluated, quantifierOmitted = quantifierOmitted)
        } else matchingTask(evaluated, taskId, quantifierOmitted))
            ?: throw TaskException(
                "no matching task; available tasks: ${if (tasks.isEmpty()) "none" else "\n$tasks"}"
            )
    val task = tasks.getTaskData(id)
    if (narrowing == task.instruction || evaluated == task.instruction) {
      selectTask(id)
      return
    }
    val intersection = intersectTask(evaluated, task.instruction, quantifierOmitted)
    if (intersection != null) {
      narrowTask(id, intersection, quantifierOmitted)
      return
    }
    if (!task.selected && selectionHandsOff(tasks, task)) {
      throw TaskException(
          "`$actor` cannot narrow task $id because selection assigns it to another Actor"
      )
    }
    val selected =
        selectTask(tasks, task)
            ?: throw TaskException(
                "task $id splits during selection; select it before narrowing its child tasks"
            )
    val selectedTask = allTasks.getTaskData(selected)
    if (selectedTask.assignee != actor) return
    narrowTask(
        id,
        intersectTask(evaluated, selectedTask.instruction, quantifierOmitted) ?: evaluated,
        quantifierOmitted,
    )
  }

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
    // TODO(ENGINE_CONTRACT): Reject Player selection while ordinary Admin work remains.
    requireNoOtherSelectedTask(task.id)
    if (task.deferred) {
      throw TaskException("task ${task.id} is deferred until no earlier work remains: $task")
    }
    if (task.selected) return task.id
    val resolved = instructor.resolve(task.instruction, worldGainNarrowing = true)
    applyResolution(queue, task, resolved, then = task.then)
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

  private fun applyResolution(
      queue: TaskQueue,
      original: Task,
      resolved: InstructionTree,
      then: InstructionGroup?,
  ) {
    val group = InstructionGroup.of(resolved)
    // A group is independent work even when it currently contains only one instruction.
    if (resolved !is InstructionGroup && group.size == 1) {
      var updated =
          taskQueues.normalizeForSelection(
              original.copy(
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
      // Structural completion admits ordinary pending work and fulfills the selected task. No child
      // inherits selection; a later player input must select whichever child comes next.
      taskQueues.addTasks(
          group,
          original.assignee,
          original.cause,
      )
      handleTask(queue, original.copy(then = then))
    }
  }

  /**
   * Selects and executes [taskId] for this Actor, applying immediate effects and checking
   * invariants. Resulting ordinary tasks remain queued. A change assigned to another Actor remains
   * selected for that Actor after the temporary execution attempt described by [selectTask].
   *
   * An instruction still requiring a choice throws. This method does not restore its own failures;
   * the client can supply an enclosing [Timeline.atomic] block. For example, rejecting removal of
   * an opponent's protected plant can leave the plant gone until the caller restores the attempt.
   */
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
        )
    newTasks.forEach(taskQueues::addTasks)
    handleTask(queue, selectedTask)
  }

  // TODO(ENGINE_CONTRACT): Review this attempt to execute the receiving Actor's later work inside
  // the current call. Proposed legality checks only the submitted instruction's own transaction.
  private fun validateConcreteHandoff(taskId: TaskId) {
    try {
      timeline.atomic {
        val task = allTasks.getTaskData(taskId)
        executeSelectedTask(queueForAnyTask(taskId), taskId, task.assignee)
        throw ConcreteHandoffValidated
      }
    } catch (_: ConcreteHandoffValidated) {
      // The target Actor can execute it; the nested attempt restored the handed-off task.
    }
  }

  /**
   * Finds the task described by [narrowing], supplies that instruction, and executes it for this
   * Actor. [taskId] selects an exact pending task; [contextClass] restricts matching to tasks
   * caused by that context class. [quantifierOmitted] preserves the pending quantifier when
   * appropriate. An enclosing gate or `/` metric may be omitted, but the task still checks it
   * against current state.
   *
   * With [executeSubmittedGroup], also attempts newly added tasks in this Actor's queue, including
   * triggered consequences and THEN continuations, until a task remains selected. With
   * [combineScalars], accepts a signed total matching several concrete changes to the same target,
   * executing gains before removals; newly caused work does not join that total. These command
   * conveniences and their public parameters still need justification.
   *
   * A change assigned to another Actor stays selected after the attempt described by [selectTask].
   * Without [executeSubmittedGroup], later queued tasks remain pending. A later failure can leave
   * earlier changes unless an enclosing [Timeline.atomic] block restores them.
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
    if (
        combineScalars &&
            taskId == null &&
            tasks.selectedTask() == null &&
            executeScalarSum(evaluated, quantifierOmitted, contextClass)
    )
        return
    val id = matchingTask(evaluated, taskId, quantifierOmitted, contextClass)
    if (id == null) {
      throw TaskException(
          "no matching task; available tasks: ${if (tasks.isEmpty()) "none" else "\n$tasks"}"
      )
    }
    val tasksBefore = tasks.ids()
    val task = tasks.getTaskData(id)
    val intersection = intersectTask(evaluated, task.instruction, quantifierOmitted)
    if (intersection != null) {
      requireNoOtherSelectedTask(id)
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

  // TODO(ENGINE_CONTRACT): Review this multi-task shortcut; the proposed clients submit explicit
  // tasks.
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
    val target = first.gaining ?: first.removing
    fun sameScalarTarget(change: Change): Boolean =
        if (first.gaining != null && first.removing != null) sameChange(change)
        else
            (change.gaining == target && change.removing == null ||
                change.removing == target && change.gaining == null)
    fun signedCount(change: Change): Long {
      val count = (change.count as ActualScalar).value.toLong()
      return if (change.gaining == null) -count else count
    }
    val total = changes.sumOf(::signedCount)
    val matches =
        tasks
            .extract { it }
            .filter { contextClass == null || it.cause?.context?.className == contextClass }
            .mapNotNull { task ->
              val resolved =
                  try {
                    instructor.resolve(task.instruction)
                  } catch (_: NotNowException) {
                    (task.instruction as? Change)?.takeUnless { it.isAbstract(reader) }
                        ?: return@mapNotNull null
                  }
              (resolved as? Change)?.takeIf(::sameScalarTarget)?.let {
                task.copy(instruction = it)
              }
            }
    if (matches.size < 2) return false
    val amounts = matches.map { it.instruction as Change }
    if (amounts.map { it.quantifier }.distinct().size != 1) return false
    if (!quantifierOmitted && amounts.any { it.quantifier != first.quantifier }) return false
    if (amounts.any { it.count !is ActualScalar }) return false
    if (amounts.sumOf(::signedCount) != total) return false
    matches
        .sortedBy { (it.instruction as Change).gaining == null }
        .forEach { doTask(it.instruction, taskId = it.id) }
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

      // A failed gate or live refinement can still identify the intended task. Let normal narrowing
      // report which requirement failed instead of replacing it with a generic no-task match.
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
    val typedZero = narrowing is Change && narrowing.count == ActualScalar(0)
    val effective =
        effectiveNarrowing(narrowing, existing, quantifierOmitted, info, intersect = true)
    effective.intersect(existing, reader.classTable, info)?.let {
      // A shared no-op does not establish that the submitted Type identifies this task.
      return it.takeUnless { typedZero && it == NoOp }
    }
    if (existing is Gated && info.has(existing.gate)) {
      effective.intersect(existing.inner, reader.classTable, info)?.let {
        return Gated.createTree(existing.gate, it)
      }
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
          head
              ?.takeUnless { typedZero && it == NoOp }
              ?.takeIf { selectFirstStageOrNull(existing, it) != null }
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
          // particular, do not turn an unrelated mandatory call into an optional Ok match.
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

  /**
   * Attempts [id] as a failure-atomic task command. If it needs a choice, is currently unavailable,
   * or must be completed by another Actor, restores the attempt and returns normally with the task
   * pending. Other failures restore the attempt and propagate.
   *
   * A successful attempt retains the task's immediate effects and queues resulting work. It does
   * not handle other pending tasks or promise that the rest of the game action can finish.
   */
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

  /**
   * Attempts the task described by [narrowing], or the exact [taskId] when supplied, with failure
   * restoration. [quantifierOmitted] and [executeSubmittedGroup] have the meanings described by
   * [doTask].
   *
   * Missing choices or currently unavailable work during execution restore the attempt and return
   * normally. Current limitation: preliminary `/` metric evaluation happens before that handling,
   * so an unavailable instruction there propagates its error. An outstanding selected task also
   * restores it. Invalid selection, invalid narrowing, and other execution failures restore the
   * attempt and propagate. Newly queued ordinary work can remain.
   */
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

  // Unlike tryTask, reports unavailable selected work as a failed task.
  /**
   * Attempts this Actor's selected task. Returns true if that task finishes, or false if further
   * choices remain. Throws when no progress is possible for another reason; current
   * unavailable-work errors are reported as [DeadEndException].
   *
   * Returns false when another Actor owns the selection. Current limitations: a missing selection
   * produces an implementation exception. Rejected execution needs an enclosing [Timeline.atomic]
   * block for restoration. Its need as public API requires review.
   */
  // TODO(ENGINE_CONTRACT): Investigate removing this public selected-task retry helper.
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
