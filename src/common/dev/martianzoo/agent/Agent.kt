package dev.martianzoo.agent

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.Multiset
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult
import kotlin.reflect.KClass

/** The single, fully permissive mutation authority for one Actor in a World. */
public interface Agent {

  // READ OPERATIONS

  public val actor: Actor

  /** The current World view used to interpret this Actor's tasks. */
  public val reader: GameReader

  /** Tasks currently assigned to this Actor. */
  public val tasks: TaskQueue

  /**
   * Parses and preprocesses [text]. Preprocessing may change its major kind; callers that require a
   * particular result kind should use [parse].
   */
  public fun parseAs(type: KClass<out PetElement>, text: String): PetElement

  public fun has(requirement: String): Boolean

  /** Counts [metric], allowing explicit `EVAL` of metric properties in this Actor's context. */
  public fun count(metric: String): Int

  /** Returns each matching component's exact type expression, preserving multiplicity. */
  public fun list(type: String): Multiset<Expression>

  public fun resolve(expression: String): Type

  // Purple mode (and below)

  /** Creates an independent, caller-held form for one of this Actor's tasks. */
  public fun fillInTask(taskId: TaskId): TaskForm

  /**
   * Narrows this Actor's selected task and resolves it again. A partial narrowing remains selected;
   * a concrete result executes before this call returns unless `BY` or `System` routing assigns it
   * to another Actor.
   *
   * @param [narrowing] the new instruction tree; may be abstract or a grouped arm selected from an
   *   `OR`; a group replaces this one task with one task per member; if identical to the current
   *   abstract instruction the task remains unchanged; an omitted quantifier retains a stronger
   *   pending quantifier when the Class default would weaken it
   * @throws [TaskException] if this Actor has no selected task
   * @throws [NarrowingException] if [narrowing] does not narrow the selected task's instruction
   */
  public fun narrowTask(narrowing: String): TaskResult

  /** Tells whether [selectTask] will complete normally. */
  public fun canSelectTask(taskId: TaskId): Boolean

  /** Tells whether this Actor can select and finish [taskId] without a choice or handoff. */
  public fun canExecuteTask(taskId: TaskId): Boolean

  /**
   * Selects one pending task and resolves its instruction against the current World. An abstract
   * result remains selected for later [narrowTask] calls. Selection may assign the task to its
   * selection assignee, and concrete instruction-side `BY` may assign it again. This call stops at
   * either handoff; otherwise a concrete result executes before it returns.
   *
   * If resolution produces independent instructions, selecting the structural task completes it and
   * admits those instructions as ordinary pending siblings.
   *
   * @throws [TaskException] if no task with id [taskId] exists, or if any other task is already
   *   selected
   * @throws [NotNowException] if the selected task cannot execute in the current World
   */
  public fun selectTask(taskId: TaskId): TaskResult

  /**
   * Selects the single pending task whose current instruction is [instruction]. Equivalent tasks
   * that differ only by id are interchangeable.
   */
  public fun selectTask(instruction: String): TaskResult

  /**
   * Carries out the task matched by the source-level [narrowing] instruction tree. A grouped tree
   * can select a grouped choice and replace the matched task with independent tasks; preprocessing
   * can produce the same replacement, for example when atomizing a multi-step global parameter
   * gain. Explicitly submitted grouped instructions execute as one bundled command; siblings
   * exposed only by preprocessing or resolution remain ordinary pending tasks. Selects and resolves
   * the matched task first if necessary. As part of this, executes triggered instructions from
   * *automatic* effects, enqueues tasks for queued effects and any contents of [Task.then], and
   * removes the original task from the game's task queue. Throws an exception if any of this fails.
   *
   * The submitted constraints are intersected with the task: each can supply choices left open by
   * the other. A selected task always wins. Otherwise, exactly one distinct task must intersect,
   * including tasks the submission would not strictly narrow; fully identical tasks remain
   * interchangeable. When the narrowing omits a quantifier and its Class default would weaken the
   * pending task's quantifier, the pending quantifier is retained; an explicitly written quantifier
   * must be compatible with the task's quantifier.
   *
   * If selecting the task without the submitted narrowing would hand it to another Actor, this
   * command fails. Use [selectTask] for that selection-only handoff; the receiving Actor can then
   * narrow or execute the selected task.
   *
   * This command also fails if the submitted narrowing makes a `System` change concrete for Admin.
   * Commit that choice with [narrowTask] or [fillInTask] instead.
   *
   * @throws [NotFullySpecifiedException] if the task is abstract
   * @throws [NotNowException] if the task can't currently be resolved
   */
  public fun doTask(narrowing: String): TaskResult

  /**
   * Executes an atomic batch against this Actor's pending tasks using [OperationScope.doTasks]
   * matching and signed scalar totals. Runs configured autoexecution before, between, and after
   * submitted instructions. Returns the whole batch's changes; any failure rolls the entire batch
   * back. This may leave work pending, so callers can split an operation across multiple batches.
   */
  public fun doTasks(vararg narrowings: String): TaskResult

  /** Carries out [narrowing] against the task caused by a component of [contextClass]. */
  public fun doTask(narrowing: String, contextClass: ClassName): TaskResult

  /** Carries out [narrowing] against the task identified by [taskId]. */
  public fun doTask(narrowing: String, taskId: TaskId): TaskResult

  /**
   * Attempts [narrowing], leaving its task pending when the play is incomplete or unavailable.
   * Invalid task selection, invalid narrowing, and dead ends still throw.
   */
  public fun tryTask(narrowing: String): TaskResult

  /** Tries [narrowing] against the task identified by [taskId]. */
  public fun tryTask(narrowing: String, taskId: TaskId): TaskResult

  /**
   * Tries to select and execute [taskId], leaving it pending when the play is incomplete or
   * unavailable. Invalid task selection and dead ends still throw.
   */
  public fun tryTask(taskId: TaskId): TaskResult

  public fun autoExecNow(): TaskResult

  /**
   * Runs autoexecution to a policy-relative stable point while this Agent temporarily uses
   * [policy]. The configured [autoExecPolicy] is unchanged.
   */
  public fun autoExecNow(policy: AutoExecPolicy): TaskResult

  public var autoExecPolicy: AutoExecPolicy

  public fun startTurn(): TaskResult

  public fun inTurn(body: OperationBlock = {}): TaskResult

  /** Starts and completes an operation seeded by one or more independent instructions. */
  public fun runOperation(initialInstructions: String, body: OperationBlock = {}): TaskResult

  /** Starts a resumable operation seeded by one or more independent instructions. */
  public fun beginOperation(initialInstructions: String, body: OperationBlock = {}): TaskResult

  public fun continueOperation(body: OperationBlock = {}): TaskResult

  public fun completeOperation(body: OperationBlock = {}): TaskResult

  /** Adds a manual task for the given [instruction], but does not select or execute it. */
  public fun addTasks(instruction: String, firstCause: Cause? = null): List<TaskId>

  /** Removes the identified task ex-machina. */
  public fun dropTask(taskId: TaskId): TaskRemovedEvent

  /**
   * Atomically applies concrete corrections, constructs required parts, and removes dependents.
   * Runs automatic effects and checks every applicable count invariant. Queued effects, task
   * settlement, and idle cleanup are omitted.
   */
  public fun sneak(changes: String, fakeCause: Cause? = null): TaskResult

  public interface OperationScope {
    public val tasks: TaskQueue
    public val reader: GameReader

    /**
     * Selects a pending task whose current instruction equals [instruction], then runs automatic
     * work. An abstract task remains selected for [narrowTask]; a concrete task may execute or
     * hand off. See [Agent.selectTask].
     */
    public fun selectTask(instruction: String)

    /**
     * Narrows this Actor's selected task and runs automatic work. A concrete `System` result passes
     * to Admin for execution. See [Agent.narrowTask].
     */
    public fun narrowTask(narrowing: String)

    public fun doTask(narrowing: String)

    public fun doTask(narrowing: String, contextClass: ClassName)

    public fun doTask(narrowing: String, taskId: TaskId)

    /**
     * Submits each instruction in order, including all atomic changes when a chosen amount expands
     * into a group. A concrete scalar change consumes all pending gains and removals of its type
     * when at least two have the same quantifier and their signed counts equal the submitted
     * amount. This also applies when an individual task has that amount. Gains execute before
     * removals. Each original task executes separately, preserving its effects, cause, and
     * continuation; newly created tasks are not included in the total. Otherwise, normal
     * single-task matching applies. Failure rolls back the enclosing operation.
     */
    public fun doTasks(vararg narrowings: String)

    public fun tryTask(narrowing: String)

    public fun tryTask(narrowing: String, taskId: TaskId)

    public fun autoExecNow()

    /** Runs autoexecution while this Agent temporarily uses [policy]. */
    public fun autoExecNow(policy: AutoExecPolicy)

    public fun abort(): Nothing = throw AbortTransactionException()
  }

  public companion object {
    public inline fun <reified P : PetElement> Agent.parse(text: String): P {
      val parsed = parseAs(P::class, text)
      if (parsed !is P) {
        throw IllegalStateException(
            "Preprocessing produced `$parsed`, which is not a ${P::class.simpleName}"
        )
      }
      return parsed
    }
  }
}
