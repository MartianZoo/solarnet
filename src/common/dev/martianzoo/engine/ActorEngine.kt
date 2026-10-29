package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Each
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
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Motivation
import dev.martianzoo.state.Motivation.CLIENT_REQUEST
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
 * Each public mutation restores its transaction on a [Throwable]. [Timeline.atomic] can also
 * restore an enclosing batch of completed transactions. Kotlin side effects and observer callbacks
 * are not undone.
 *
 * Player selection is refused while an ordinary Admin task can complete under the existing
 * selection probe. A selected task retains priority and can retain partial choices. Complete
 * enumeration of legal Admin choices remains under review.
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
    private val transactions: Transactions,
    private val trialWorld: () -> SpeculativeWorld,
) {
  private val allTasks: TaskQueue = gameWorld.tasks

  private object SelectionHandoffDetected : RuntimeException()

  private val possibleWorldFacts = narrowingFacts(requirementsHold = true)

  /**
   * Runs [block] and subsequent task handling, returning their combined [TaskResult]. The intended
   * synchronous behavior is to run to quiescence and report all changes made before return.
   *
   * Runs [afterChanges] and [validateCompletion] for the outermost call, inside the same restoring
   * block as [block]. Nested calls skip their own finishing steps. Agent supplies task handling and
   * application validation. This wrapper does not assign an Actor to changes; commands inside do.
   *
   * The returned result covers every retained event in the call. A [Throwable] during the request,
   * task handling, or completion validation restores the whole call's game changes before escaping.
   * [AbortTransactionException] restores the innermost block and returns normally. See
   * [Timeline.atomic] for the state covered by restoration.
   */
  // TODO(ENGINE_CONTRACT): Review whether this block-based coordinator belongs in the public API.
  // Preserve synchronous execution through quiescence and complete TaskResult reporting.
  public fun transact(
      afterChanges: () -> Unit,
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult = worldTransaction.run(block, validateCompletion, afterChanges)

  /**
   * Records [block]'s engine mutations as one transaction motivated by [motivation] rather than by
   * a client submission, attributed to this engine's [actor]. Returns [block]'s result.
   *
   * A driver performing automatic handling uses this so that history says why the work executed
   * when it did. Mutations called inside join this transaction instead of starting their own; a
   * transaction already open, or unfinished initialization, leaves the grouping unchanged. This
   * supplies provenance only: it grants no additional authority and performs no work itself.
   */
  public fun <T> transaction(motivation: Motivation, block: () -> T): T =
      transactions.perform(actor, motivation, block)

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
   * Applies [changes] as a failure-atomic correction attributed to [actor]. Recursive `::` effects
   * and final invariant checks still run; queued `:` effects and client task handling do not.
   * Returns the correction's component changes and surviving new tasks.
   *
   * This bypasses ordinary task selection and preserves pending tasks, including any selected
   * instruction. Agent adds correction recording, application validation, and the unfinished-work
   * check around this module-level primitive.
   */
  public fun exMachina(changes: InstructionGroup): TaskResult = worldTransaction.correct {
    transactions.perform(actor, CLIENT_REQUEST) { instructor.exMachina(changes, actor) }
  }

  // TASKS LAYER

  /**
   * Adds [instructions] as work initially assigned to [actor], recording [firstCause] when
   * supplied. This attributes the initial work; triggered effects record their own causes. Returns
   * the ids of tasks actually added in creation order. Normalization can discard no-ops, split
   * groups, perform dispatches, and assign concrete System changes to Admin.
   *
   * Refused while any task is selected: that task must finish before the queue changes around it.
   *
   * Adding work does not execute its ordinary task bodies. Eligible deferred work is promoted at
   * transaction completion. A failed transaction restores its additions.
   */
  // TODO(ENGINE_CONTRACT): Review whether direct queue editing belongs in the public API.
  public fun addTasks(instructions: InstructionGroup, firstCause: Cause? = null): List<TaskId> =
      transactions.perform(actor, CLIENT_REQUEST) {
        requireNoSelectedTask("add tasks")
        taskQueues.addTasks(instructions, actor, firstCause).map { it.task.id }
      }

  /**
   * Removes an existing task assigned to [actor] and returns its removal event. Does not execute
   * the removed task's instruction. Rejects a missing task or a task belonging to another Actor.
   *
   * Refused while a different task is selected. Dropping the selected task itself is permitted:
   * that is how a client abandons work it has started.
   *
   * Current limitation: removing work may promote deferred tasks and perform their dispatches, and
   * does not handle newly eligible Admin work by itself.
   */
  // TODO(ENGINE_CONTRACT): Review removing direct task deletion from the engine's public API.
  public fun dropTask(taskId: TaskId): TaskRemovedEvent =
      transactions.perform(actor, CLIENT_REQUEST) {
        requireNoSelectedTask("drop task $taskId", allowed = taskId)
        taskQueues.removeTask(tasks.getTaskData(taskId))
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

  /**
   * A selected task excludes a competing queue edit, as it already excludes a competing task
   * command: the selected task must finish before the queue it belongs to changes around it.
   * Dropping the selected task itself is how a client abandons it.
   */
  private fun requireNoSelectedTask(attempt: String, allowed: TaskId? = null) {
    val selected = allTasks.selectedTask() ?: return
    if (selected == allowed) return
    throw TaskException("cannot $attempt while task $selected is selected")
  }

  private fun requireNoOtherSelectedTask(taskId: TaskId) {
    // Check the whole task pool: another Actor's selection also excludes this task command.
    // Explicit corrections preserve the selected task; ordinary queue edits respect this exclusion.
    val already = allTasks.selectedTask()
    if (already != null && already != taskId) {
      val instr = allTasks.getTaskData(already).instruction
      throw TaskException("task $already ($instr) is selected and must finish first")
    }
    // Once the transaction has begun changing state, finish it before handling new Admin work.
    if (
        already == null &&
            !transactions.hasRetainedEvents &&
            allTasks.getTaskData(taskId).assignee != Actor.ADMIN
    ) {
      var incomplete: IncompleteAnalysisException? = null
      val admin =
          allTasks.ids().firstOrNull {
            val task = allTasks.getTaskData(it)
            task.assignee == Actor.ADMIN &&
                !task.deferred &&
                try {
                  canSelectTask(it, queueForAnyTask(it), requireConcrete = true)
                } catch (e: IncompleteAnalysisException) {
                  incomplete = incomplete ?: e
                  false
                }
          }
      if (admin != null)
          throw TaskException(
              "Admin task $admin (${allTasks.getTaskData(admin).instruction}) must finish before Player selection"
          )
      incomplete?.let { throw it }
    }
  }

  // TASK COMMANDS

  /**
   * Supplies [narrowing] for this Actor's selected task. When [quantifierOmitted] is true, retains
   * the pending task's quantifier where needed. Resolves and stores the resulting instruction
   * without consuming the task or admitting children. Actor reassignment follows [selectTask].
   * Partial choices remain on the selected task for further narrowing or execution.
   *
   * Rejects a missing selection or incompatible narrowing and restores a failed transaction.
   */
  public fun narrowTask(narrowing: InstructionTree, quantifierOmitted: Boolean = false) {
    val taskId = tasks.selectedTask() ?: throw TaskException("`$actor` has no selected task")
    transactions.perform(actor, CLIENT_REQUEST) {
      narrowSelectedTask(taskId, narrowing, quantifierOmitted)
    }
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
   * count invariants contribute to the bound; only AMAP uses them to determine its count. Mandatory
   * and optional requests keep positive choices that immediate effects may repair.
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
   * omitted quantifier when [quantifierOmitted] is true. Resolves and stores the result without
   * executing component changes, leaving the task pending for its current assignee.
   *
   * Rejects a competing selection, deferred work, an incompatible narrowing, or narrowing that
   * belongs to a different Actor after selection. Groups and `EACH` retain their parent until
   * execution. A failed transaction restores its changes.
   */
  public fun narrowTask(
      taskId: TaskId,
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
  ): Unit =
      transactions.perform(actor, CLIENT_REQUEST) {
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
          val selected = selectTask(task)
          val selectedTask = allTasks.getTaskData(selected)
          if (selectedTask.assignee != actor) {
            if (narrowsBeforeHandoff) {
              throw TaskException(
                  "`$actor` cannot narrow task $taskId because selection assigns it to " +
                      "`${selectedTask.assignee}`"
              )
            }
            return@perform
          }
          if (effectiveNarrowing is Gated && selectedTask.instruction !is Gated) {
            effectiveNarrowing =
                instructor.resolve(
                    effectiveNarrowing,
                    worldGainNarrowing = true,
                    expandEach = false,
                )
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
      selectTask(tasks.getTaskData(taskId))
      return
    }
    val continuation = prepared.selectedThen?.continuationAfterFirst() ?: task.then

    // Keep the chosen group unresolved; its children resolve against their own execution states.
    val resolved =
        instructor.resolve(effectiveNarrowing, worldGainNarrowing = true, expandEach = false)
    applyResolution(task, resolved, then = continuation)
  }

  /**
   * Checks whether this task can be selected against the current world, restoring attempted changes
   * before returning. A concrete selection also attempts execution if it stays with its current
   * Actor; a handoff or abstract selection can return true without executing. This does not handle
   * later queued tasks. Gameplay rejection or a known missing choice returns false.
   * [IncompleteAnalysisException] propagates when a required execution proof cannot finish,
   * including the Admin priority check. Other failures also propagate after restoration.
   *
   * This is not a count of legal concrete narrowings. The attempt uses a disposable World; source
   * listeners, callbacks, and application checks do not observe trial events.
   */
  // TODO(ENGINE_CONTRACT): Complete inference beyond fixed mandatory/optional changes.
  public fun canSelectTask(taskId: TaskId): Boolean = canSelectTask(taskId, tasks)

  private fun canSelectTask(
      taskId: TaskId,
      queue: TaskQueue,
      requireConcrete: Boolean = false,
  ): Boolean {
    timeline.requireAtEnd()
    if (taskId !in queue) return false
    // The Admin priority check can probe a task from a Player's engine.
    return trialWorld()
        .actorEngine(allTasks.getTaskData(taskId).assignee)
        .probeSelection(taskId, requireConcrete)
  }

  private fun probeSelection(taskId: TaskId, requireConcrete: Boolean): Boolean =
      try {
        transactions.perform(actor, CLIENT_REQUEST) {
          val original = allTasks.getTaskData(taskId)
          val selected = selectTask(original)
          val task = allTasks.getTaskData(selected)
          if (
              task.assignee == original.assignee &&
                  (requireConcrete || !task.instruction.isAbstract(reader))
          ) {
            executeSelectedTask(queueForAnyTask(selected), selected, task.assignee)
          }
        }
        true
      } catch (_: GameplayException) {
        false
      } catch (e: IncompleteAnalysisException) {
        throw e
      } catch (_: NotFullySpecifiedException) {
        false
      }

  /**
   * Checks whether this Actor can select and finish [taskId] now, restoring attempted changes
   * before returning. Returns false if further narrowing or a different Actor is needed. Newly
   * queued work is outside this check.
   *
   * Gameplay rejection or a known missing choice returns false. [IncompleteAnalysisException]
   * propagates when the engine cannot finish checking the possible narrowings; false never
   * represents that unfinished proof. Other failures also propagate after restoration. The attempt
   * runs in a disposable World and does not invalidate existing source overlays or notify source
   * listeners.
   */
  // TODO(ENGINE_CONTRACT): Complete inference beyond fixed mandatory/optional changes.
  public fun canExecuteTask(taskId: TaskId): Boolean {
    timeline.requireAtEnd()
    val trial = trialWorld()
    return try {
      trial.actorEngine(actor).doTask(taskId)
      taskId !in trial.tasks
    } catch (_: GameplayException) {
      false
    } catch (e: IncompleteAnalysisException) {
      throw e
    } catch (_: NotFullySpecifiedException) {
      false
    }
  }

  /**
   * Selects this Actor's [taskId] and resolves its instruction against current state. Concrete
   * component changes remain pending until an execution command. Groups, `EACH`, and dispatches
   * exposed by selection retain the same task. Execution admits their independent children without
   * passing selection to them and releases any continuation.
   *
   * When resolution reassigns the task, this call records the handoff without attempting the
   * receiving Actor's execution. That Actor performs the instruction in a later transaction, which
   * may fail independently of this selection.
   *
   * Rejects deferred work, a different selected task, and Player selection while the existing probe
   * finds an executable ordinary Admin task. If no Admin task is proven executable but an Admin
   * proof remains unfinished, [IncompleteAnalysisException] propagates. A failed transaction
   * restores its changes.
   */
  public fun selectTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    if (actor != task.assignee) {
      throw TaskException("`$actor` cannot select a task assigned to `${task.assignee}`")
    }
    transactions.perform(actor, CLIENT_REQUEST) {
      selectTask(tasks.getTaskData(taskId))
    }
  }

  /**
   * Selects a task compatible with [narrowing], restricted to [taskId] when supplied, and applies
   * the narrowing. [quantifierOmitted] lets an omitted quantifier come from the pending task.
   * Distinct matching tasks are rejected as ambiguous; identical tasks differing only in identity
   * and cause currently use the first match.
   *
   * Resolves and stores work as [selectTask] does, with the same priority and failure restoration.
   * Partial selection can still persist.
   */
  public fun selectTask(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      taskId: TaskId? = null,
  ): Unit =
      transactions.perform(actor, CLIENT_REQUEST) {
        val evaluated = evaluatePer(narrowing)
        val id =
            (if (taskId == null && tasks.selectedTask() == null) {
              val exact =
                  tasks
                      .extract { it }
                      .filter { it.assignee == actor && it.instruction == narrowing }
              if (exact.isNotEmpty()) uniqueMatchingTask(exact)
              else matchingTask(evaluated, quantifierOmitted = quantifierOmitted)
            } else matchingTask(evaluated, taskId, quantifierOmitted))
                ?: throw TaskException(
                    "no matching task; available tasks: ${if (tasks.isEmpty()) "none" else "\n$tasks"}"
                )
        val task = tasks.getTaskData(id)
        if (narrowing == task.instruction || evaluated == task.instruction) {
          selectTask(id)
          return@perform
        }
        val intersection = intersectTask(evaluated, task.instruction, quantifierOmitted)
        if (intersection != null) {
          narrowTask(id, intersection, quantifierOmitted)
          return@perform
        }
        if (!task.selected && selectionHandsOff(task)) {
          throw TaskException(
              "`$actor` cannot narrow task $id because selection assigns it to another Actor"
          )
        }
        val selected = selectTask(task)
        val selectedTask = allTasks.getTaskData(selected)
        if (selectedTask.assignee != actor) return@perform
        narrowTask(
            id,
            intersectTask(evaluated, selectedTask.instruction, quantifierOmitted) ?: evaluated,
            quantifierOmitted,
        )
      }

  private fun selectTask(task: Task): TaskId {
    requireNoOtherSelectedTask(task.id)
    if (task.deferred) {
      throw TaskException("task ${task.id} is deferred until no earlier work remains: $task")
    }
    if (task.selected) return task.id
    val resolved =
        instructor.resolve(task.instruction, worldGainNarrowing = true, expandEach = false)
    applyResolution(task, resolved, then = task.then)
    return task.id
  }

  private fun selectionHandsOff(task: Task): Boolean {
    return try {
      timeline.atomic {
        val selected = selectTask(task)
        if (allTasks.getTaskData(selected).assignee != actor) throw SelectionHandoffDetected
        throw AbortTransactionException()
      }
      false
    } catch (_: SelectionHandoffDetected) {
      true
    }
  }

  private fun applyResolution(
      original: Task,
      resolved: InstructionTree,
      then: InstructionGroup?,
  ) {
    var updated =
        taskQueues.normalizeForSelection(
            original.copy(selected = true, instruction = resolved, then = then)
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
  }

  private fun admitChildren(queue: TaskQueue, parent: Task, group: InstructionGroup): List<TaskId> {
    val children =
        taskQueues
            .addTasks(group, parent.assignee, parent.cause, includeDispatchedTasks = false)
            .map { it.task.id }
    handleTask(queue, parent)
    return children
  }

  /**
   * Selects and executes [taskId] for this Actor, applying immediate effects and checking
   * invariants. Executing `EACH` admits its children and consumes the parent; the children remain
   * queued at their ordinary priorities. A change assigned to another Actor remains selected for
   * that Actor to perform in a later transaction.
   *
   * A fixed mandatory or optional change, or OR of those changes, can infer its unique successful
   * concrete target or source/destination pair by trying its complete finite domain in disposable
   * Worlds. Optional changes also try zero and every positive count up to the requested amount.
   * Repeated concrete requests across alternatives count once. Multiple successful choices throw
   * [NotFullySpecifiedException]; an unfinished trial throws [IncompleteAnalysisException]. Trials
   * include immediate effects and task admission/promotion, but do not execute subsequently queued
   * work. A failed transaction restores its changes; for example, rejecting removal of an
   * opponent's protected plant leaves that plant intact.
   */
  public fun doTask(taskId: TaskId) {
    transactions.perform(actor, CLIENT_REQUEST) { doTask(tasks, taskId) }
  }

  private fun doTask(queue: TaskQueue, taskId: TaskId): Task {
    val original = queue.getTaskData(taskId)
    val selected = selectTask(original)
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
  ): List<TaskId> {
    val selectedTask = queue.getTaskData(taskId)
    check(selectedTask.selected)
    check(selectedTask.assignee == executingActor)
    val instruction =
        when (val tree = selectedTask.instruction) {
          is InstructionGroup -> return admitChildren(queue, selectedTask, tree)
          is Instruction -> tree
        }
    if (instruction is Each) {
      val children = instructor.resolve(instruction, worldGainNarrowing = true)
      return admitChildren(queue, selectedTask, InstructionGroup.of(children))
    }
    if (instruction.isAbstract(reader)) {
      val chosen = successfulNarrowing(taskId, instruction, executingActor)
      narrowSelectedTask(taskId, chosen, quantifierOmitted = false)
      if (allTasks.getTaskData(taskId).assignee == executingActor) {
        return executeSelectedTask(queueForAnyTask(taskId), taskId, executingActor)
      }
      return emptyList()
    }
    val newTasks =
        instructor.executeResolved(
            instruction,
            selectedTask.cause,
            executingActor,
        )
    newTasks.forEach(taskQueues::addTasks)
    handleTask(queue, selectedTask)
    return emptyList()
  }

  /** Complete finite type domains only; unsupported or unfinished analysis remains unresolved. */
  private fun successfulNarrowing(
      taskId: TaskId,
      instruction: Instruction,
      executingActor: Actor,
  ): Instruction {
    var incomplete: IncompleteAnalysisException? = null
    fun concreteTargets(target: Expression?): List<Expression?> =
        if (target == null) listOf(null)
        else
            reader.classTable
                .allConcreteSubtypes(reader.resolve(target))
                .map { it.expression }
                .distinct()
                .toList()

    fun candidates(request: InstructionTree): Sequence<Instruction> =
        sequence<Instruction> {
          if (request == NoOp) {
            yield(NoOp)
            return@sequence
          }
          if (request is Or) {
            for (alternative in request.instructions) yieldAll(candidates(alternative))
            return@sequence
          }
          if (
              request !is Change ||
                  request.count !is ActualScalar ||
                  request.quantifier !in
                      setOf(Instruction.Quantifier.MANDATORY, Instruction.Quantifier.OPTIONAL) ||
                  (request is Transmute && request.fromEx is Compact)
          ) {
            incomplete =
                incomplete
                    ?: IncompleteAnalysisException("transaction analysis does not cover `$request`")
            return@sequence
          }
          val gains = concreteTargets(request.gaining)
          val removals = concreteTargets(request.removing)
          val count = (request.count as ActualScalar).value
          val counts =
              if (request.quantifier == Instruction.Quantifier.OPTIONAL) 1..count else count..count
          if (request.quantifier == Instruction.Quantifier.OPTIONAL) yield(NoOp)
          for (candidateCount in counts) for (gaining in gains) for (removing in removals) {
            // Equal concrete types cannot form a transmutation (Pets rule L2-1).
            if (gaining != null && gaining == removing) continue
            val candidate =
                Change.change(
                    gaining,
                    removing,
                    candidateCount,
                    Instruction.Quantifier.MANDATORY,
                ) as Change
            if (
                request is Transmute &&
                    request.fromEx is Full &&
                    !candidate.narrows(request, possibleWorldFacts)
            )
                continue
            yield(candidate)
          }
        }
    val trial = trialWorld()
    var winner: Instruction? = null
    var invalidCandidate: ExpressionException? = null
    val rejections = mutableListOf<GameplayException>()
    // An OR may reach the same concrete instruction through multiple arms (notably optional zero).
    for (candidate in candidates(instruction).distinct()) {
      if (candidate is Change) {
        try {
          instructor.validateConcreteChange(candidate)
        } catch (e: ExpressionException) {
          invalidCandidate = invalidCandidate ?: e
          continue
        }
      }
      trial.reset()
      try {
        trial.actorEngine(executingActor).doTask(candidate, taskId = taskId)
      } catch (e: NotFullySpecifiedException) {
        incomplete =
            incomplete
                ?: IncompleteAnalysisException(
                    "candidate `$candidate` could not finish its requested transaction: ${e.detail}",
                    e.sourceLocation,
                )
        continue
      } catch (e: GameplayException) {
        rejections += e
        continue
      }
      if (taskId in trial.tasks && trial.tasks.getTaskData(taskId).assignee == executingActor) {
        incomplete =
            incomplete
                ?: IncompleteAnalysisException(
                    "candidate `$candidate` did not finish its transaction"
                )
        continue
      }
      if (winner != null)
          throw NotFullySpecifiedException("multiple successful concrete narrowings")
      winner = candidate
    }
    incomplete?.let { throw it }
    winner?.let {
      return it
    }
    if (rejections.isEmpty()) {
      invalidCandidate?.let { throw it }
      instructor.resolve(instruction) // Retain the existing uninhabited-type diagnostic.
      throw DeadEndException("no compatible concrete narrowing for `$instruction`")
    }
    val detail = rejections.map { it.detail }.distinct().sorted().joinToString("; ")
    // The iteration order cannot decide whether Admin waits or rejects. A currently unavailable
    // candidate is not proof that the whole task can never succeed after another task changes
    // state.
    if (rejections.all { it is DeadEndException }) throw DeadEndException(detail)
    throw NotNowException("no successful concrete narrowing: $detail")
  }

  /**
   * Finds the task described by [narrowing], supplies that instruction, and executes it for this
   * Actor. [taskId] selects an exact pending task; [contextClass] restricts matching to tasks
   * caused by that context class. [quantifierOmitted] preserves the pending quantifier when
   * appropriate. An enclosing gate or `/` metric may be omitted, but the task still checks it
   * against current state.
   *
   * A submitted group executes its requested members still assigned to this Actor after executing
   * the parent to admit them. Members assigned to another Actor remain pending for that Actor. A
   * member that becomes selected for another Actor stops execution of the remaining members.
   * Reactions and continuations remain queued. Separate pending tasks must be submitted
   * individually; a net resource total does not identify their execution order.
   *
   * A change assigned to another Actor stays selected without attempting that Actor's execution.
   * Later queued tasks remain pending. A failed transaction restores all changes made by this
   * command.
   */
  public fun doTask(
      narrowing: InstructionTree,
      quantifierOmitted: Boolean = false,
      taskId: TaskId? = null,
      contextClass: ClassName? = null,
  ): Unit =
      transactions.perform(actor, CLIENT_REQUEST) {
        val evaluated = evaluatePer(narrowing)
        val id = matchingTask(evaluated, taskId, quantifierOmitted, contextClass)
        if (id == null) {
          throw TaskException(
              "no matching task; available tasks: ${if (tasks.isEmpty()) "none" else "\n$tasks"}"
          )
        }
        val task = tasks.getTaskData(id)
        val intersection = intersectTask(evaluated, task.instruction, quantifierOmitted)
        if (intersection != null) {
          requireNoOtherSelectedTask(id)
          narrowSelectedTask(id, intersection, quantifierOmitted)
        } else {
          val selected = selectTask(task)
          val selectedTask = queueForAnyTask(selected).getTaskData(selected)
          if (selectedTask.assignee != actor) return@perform
          val instruction = selectedTask.instruction
          narrowTask(
              intersectTask(evaluated, instruction, quantifierOmitted) ?: evaluated,
              quantifierOmitted,
          )
        }
        val selectedTask = allTasks.getTaskData(id)
        if (selectedTask.assignee != actor) return@perform
        val children = executeSelectedTask(tasks, id)
        if (evaluated is InstructionGroup) {
          for (child in children) {
            if (allTasks.selectedTask() != null) break
            if (child in tasks) doTask(child)
          }
        }
      }

  private fun evaluatePer(instruction: InstructionTree): InstructionTree =
      if (instruction is Per) instructor.resolve(instruction) else instruction

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
          // Shared numeric choices must be supplied as X, not inferred from a stage total.
          if (then.descendantsOfType<XScalar>().isNotEmpty()) return@mapNotNull null
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

  private fun queueForAnyTask(taskId: TaskId): TaskQueue =
      gameWorld.tasksFor(allTasks.getTaskData(taskId).assignee)
}
