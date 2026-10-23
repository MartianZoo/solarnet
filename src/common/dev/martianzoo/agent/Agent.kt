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
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult
import kotlin.reflect.KClass

/**
 * Actor-scoped access to a live world. Queries and submitted choices are interpreted for [actor],
 * and [tasks] exposes that actor's current pending work. Holding an Agent grants the ability to
 * submit that actor's choices; the engine does not authenticate the human or program holding it.
 *
 * A gameplay call may make several engine calls. Mandatory Admin handling runs before and after
 * Player decisions, including nested calls, while [autoExecPolicy] controls Player work. A selected
 * task retains priority; an Admin task waiting on Player payment can remain pending. Complete
 * enumeration of legal Admin choices is not implemented.
 *
 * A returned [TaskResult] covers every retained change during that call, including Admin handling
 * and completion-callback work. Failures normally restore the call's changes. Completion callbacks
 * run after that restoring block, with separate failure handling. [sneak] deliberately omits task
 * handling; [addTasks] and [dropTask] handle Admin work without choosing Player tasks.
 *
 * The operation API groups work across calls: [beginOperation] and [continueOperation] allow tasks
 * to remain, while [runOperation] and [completeOperation] check whether that operation has
 * finished. An operation in those APIs is not the proposed engine transaction.
 */
public interface Agent {

  // READ OPERATIONS

  /** Actor to whom this Agent attributes queries and submitted choices. */
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

  /**
   * Parses [requirement] in this actor's context and reports whether the current world meets it.
   */
  public fun has(requirement: String): Boolean

  /** Counts [metric], allowing explicit `EVAL` of metric properties in this Actor's context. */
  public fun count(metric: String): Int

  /** Returns each matching component's exact type expression, preserving multiplicity. */
  public fun list(type: String): Multiset<Expression>

  /** Parses [expression] and resolves it to a type in this actor's current world. */
  public fun resolve(expression: String): Type

  // TASK CHOICES

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

  /**
   * Probes whether [selectTask] would complete normally for [taskId] now. A missing or unselectable
   * task yields false. The probe rolls back its attempted selection and any resulting effects.
   * Tentative events can invalidate overlays based on this World. Resource-limit failures propagate
   * to the caller.
   */
  // TODO(ENGINE_CONTRACT): investigate removing speculative probes from this API. A successful
  // selection can remain abstract and must not be called a legal fully concrete narrowing.
  public fun canSelectTask(taskId: TaskId): Boolean

  /**
   * Probes whether this actor can select and finish [taskId] now without another choice or handoff.
   * A missing or unfinished task yields false. The probe restores gameplay state, but tentative
   * events can invalidate overlays based on this World. Resource-limit failures propagate to the
   * caller.
   */
  // TODO(ENGINE_CONTRACT): investigate whether callers need this execution probe. If retained,
  // define its answer using this instruction's whole transaction, without checking spawned tasks.
  public fun canExecuteTask(taskId: TaskId): Boolean

  /**
   * Selects one pending task and resolves its instruction against the current World. An abstract
   * result remains selected for later [narrowTask] calls. Concrete `BY` or System routing may
   * assign it to another Actor. The call's task handling then runs, so that Actor's policy decides
   * whether the task finishes before this call returns.
   *
   * If resolution produces independent instructions, selecting the structural task completes it and
   * admits those instructions as ordinary pending siblings.
   *
   * @throws [TaskException] if no task with id [taskId] exists, or if any other task is already
   *   selected
   * @throws [NotNowException] if the selected task cannot execute in the current World
   */
  // TODO(ENGINE_CONTRACT): Investigate
  // submitting a concrete instruction with selection instead of persisting unfinished choices.
  public fun selectTask(taskId: TaskId): TaskResult

  /**
   * Selects this Actor's sole pending task that is not [deferred][Task.deferred], using
   * [selectTask]'s resolution and handoff rules. When [x] is supplied, binds the task's X scalars
   * to that value before selection, using [Task.bindXTo] and [selectTask]'s narrowing rules.
   * Otherwise selects without narrowing. Throws [TaskException] without changing the World if this
   * Actor has no such task or more than one.
   */
  public fun selectOnlyTask(x: Int? = null): TaskResult {
    val id =
        tasks.matching { !it.deferred }.singleOrNull()
            ?: throw TaskException("Expected exactly one pending task that is not deferred")
    return if (x == null) selectTask(id) else selectTask(tasks.getTaskData(id).bindXTo(x))
  }

  /**
   * Finds the unique pending task compatible with [instruction], selects it, and commits the
   * supplied narrowing. An unchanged instruction only selects. A nontrivial narrowing fails if
   * selection alone transfers the task to another Actor or splits it before the narrowing can be
   * applied. Such failures leave the World unchanged; select the original task first, then narrow
   * its resulting tasks. Equivalent tasks that differ only by id are interchangeable.
   */
  public fun selectTask(instruction: String): TaskResult

  /** Selects [task] by id and submits its instruction as a narrowing of the live task. */
  public fun selectTask(task: Task): TaskResult

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
   * A submitted zero count retains its Type constraint: `0 Foo` can match `Foo / metric` when the
   * metric evaluates to zero. An unrelated `Ok` alternative does not identify a matching task.
   * Executing a zero count emits no component changes.
   *
   * Selection or narrowing may hand the task to another Actor, including Admin for a concrete
   * `System` change. Runs configured autoexecution and succeeds only if that task finishes; a
   * pending handoff fails and rolls back this call. Use [selectTask], [narrowTask], or [fillInTask]
   * when the receiving Actor must finish it later. Consequences may remain as ordinary pending
   * work.
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
   * Selects and finishes [taskId], including configured autoexecution after an Actor handoff. Fails
   * and rolls back this call if the selected task remains incomplete.
   */
  public fun doTask(taskId: TaskId): TaskResult

  /**
   * Attempts [narrowing], then runs configured task handling. Missing choices or unavailable
   * execution restore the engine attempt. A task left selected, including a handoff to another
   * Actor, also restores the attempt. Subsequent task handling can still execute that task or
   * reject the call. Invalid selection, invalid narrowing, and other failures propagate, including
   * `Die` and failures encountered while handling Admin work. Currently unavailable work discovered
   * during preliminary `/` metric evaluation also propagates its error.
   */
  public fun tryTask(narrowing: String): TaskResult

  /** Tries [narrowing] against the task identified by [taskId]. */
  public fun tryTask(narrowing: String, taskId: TaskId): TaskResult

  /**
   * Attempts to select and execute [taskId], then runs configured task handling. Missing choices,
   * unavailable execution, or an unfinished handoff restore the engine attempt; subsequent task
   * handling can still execute that task or reject the call. Invalid selection and other failures
   * propagate, including `Die` and failures encountered while handling Admin work.
   */
  public fun tryTask(taskId: TaskId): TaskResult

  /**
   * Runs eligible automatic tasks to a stable point under [autoExecPolicy] and returns their
   * combined changes. A failure rolls back this invocation.
   */
  public fun autoExecNow(): TaskResult

  /**
   * Runs autoexecution to a policy-relative stable point under a scoped [policy] override. The
   * configured [autoExecPolicy] is unchanged.
   */
  public fun autoExecNow(policy: AutoExecPolicy): TaskResult

  /**
   * Policy used for this Agent's task handling. Changing it to a different policy other than NONE
   * immediately runs task handling. Assigning NONE or the current value runs nothing. The policy
   * belongs to this Agent, while task handling can affect the shared world. Admin accepts only
   * [AutoExecPolicy.EAGER], including scoped overrides; other values throw.
   */
  public var autoExecPolicy: AutoExecPolicy

  /**
   * Seeds an operation with [initialInstructions] and requires it to complete after `body` and
   * configured task handling. Other already-pending tasks may remain, but a selected task prevents
   * the new operation from starting. Any failure or remaining work created by this operation rolls
   * back the whole call.
   *
   * Current limitation: an ordinary nested call checks completion inside its body, but the shared
   * coordinator skips its separate validator after subsequent task handling. The outer call
   * controls that later work and validation.
   */
  // TODO(ENGINE_CONTRACT): investigate removing this operation lifecycle and its completion
  // requirements. Running to quiescence does not establish that a whole operation is complete;
  // these methods additionally infer completion from pending cleanup work across multiple calls.
  public fun runOperation(initialInstructions: String, body: OperationBlock = {}): TaskResult

  /**
   * Starts a resumable operation seeded by one or more independent instructions. For example,
   * `beginOperation("X Plant")` may return with the quantity choice still pending; a later call can
   * narrow that task before [completeOperation] finishes the operation. The world must have no
   * pending tasks when this call starts.
   */
  public fun beginOperation(initialInstructions: String, body: OperationBlock = {}): TaskResult

  /**
   * Runs `body` and configured autoexecution atomically, allowing tasks to remain pending for a
   * later call. A failure rolls back this call's changes.
   */
  public fun continueOperation(body: OperationBlock = {}): TaskResult

  /**
   * Runs `body` atomically and requires the world's operation work to be complete afterward. An
   * incomplete or failed operation rolls back this call. Nested calls have the same limitation on
   * final completion validation described by [runOperation].
   */
  public fun completeOperation(body: OperationBlock = {}): TaskResult

  /**
   * Adds work for [instruction] and handles eligible Admin tasks. Returned IDs identify the tasks
   * initially added; Admin may already have completed them. Player tasks remain pending.
   * [firstCause] attributes the initial tasks; later effects record their own causes.
   */
  public fun addTasks(instruction: String, firstCause: Cause? = null): List<TaskId>

  /**
   * Removes [taskId] from this actor's pending queue without executing its instruction. The
   * returned event records that removal; an absent or other actor's task fails. Eligible Admin work
   * is handled before return.
   */
  public fun dropTask(taskId: TaskId): TaskRemovedEvent

  /**
   * Atomically applies concrete corrections, constructs required parts, and removes dependents.
   * Runs `::` effects and checks every applicable count invariant. Queued effects, task handling,
   * and idle cleanup are omitted.
   */
  public fun sneak(changes: String, fakeCause: Cause? = null): TaskResult

  /**
   * Commands available inside an [OperationBlock]. They act on the enclosing Agent and contribute
   * to the enclosing call's result. A command failure restores that call's earlier changes. The
   * scope's task and reader views reflect changes made earlier in the same block.
   */
  public interface OperationScope {
    /** This actor's current pending-task view. */
    public val tasks: TaskQueue
    /** Current world view for interpreting the actor's tasks. */
    public val reader: GameReader

    /**
     * Selects and narrows a pending task compatible with [instruction], then runs automatic work.
     * An abstract result remains selected for [narrowTask]; a concrete result may execute or hand
     * off. See [Agent.selectTask] for selection and failure rules.
     */
    public fun selectTask(instruction: String)

    /** Selects [task] by id and submits its instruction as a narrowing of the live task. */
    public fun selectTask(task: Task)

    /**
     * Selects this Actor's sole pending task and runs automatic work; see [Agent.selectOnlyTask].
     */
    public fun selectOnlyTask(x: Int? = null)

    /**
     * Narrows this Actor's selected task and runs automatic work. A concrete `System` result passes
     * to Admin for execution. See [Agent.narrowTask].
     */
    public fun narrowTask(narrowing: String)

    /** Executes [narrowing] using [Agent.doTask]'s matching and completion rules. */
    public fun doTask(narrowing: String)

    /** Executes [narrowing] against work caused by a component of [contextClass]. */
    public fun doTask(narrowing: String, contextClass: ClassName)

    /** Executes [narrowing] against the pending task identified by [taskId]. */
    public fun doTask(narrowing: String, taskId: TaskId)

    /**
     * Submits each instruction in order, including all component changes when a chosen amount
     * expands into a group. A concrete scalar change consumes all pending gains and removals of its
     * type when at least two have the same quantifier and their signed counts equal the submitted
     * amount, including zero when gains and removals cancel. This also applies when an individual
     * task has that amount. Gains execute before removals. Each original task executes separately,
     * preserving its effects, cause, and continuation; newly created tasks are not included in the
     * total. Otherwise, normal single-task matching applies. Failure rolls back the enclosing
     * operation.
     */
    // TODO(ENGINE_CONTRACT): investigate removing implicit scalar matching and gain-before-removal
    // ordering. Explicit client choices should identify which pending instructions to perform.
    public fun doTasks(vararg narrowings: String)

    /** Tries [narrowing], leaving incomplete or unavailable work pending. */
    public fun tryTask(narrowing: String)

    /** Tries [narrowing] against the pending task identified by [taskId]. */
    public fun tryTask(narrowing: String, taskId: TaskId)

    /** Runs eligible automatic work now under this Agent's configured policy. */
    public fun autoExecNow()

    /**
     * Runs autoexecution under a scoped [policy] override without changing the configured policy.
     */
    public fun autoExecNow(policy: AutoExecPolicy)

    /**
     * Runs automatic work before, between, and after [body]'s commands under [policy]. The
     * configured [Agent.autoExecPolicy] is never changed, including while [body] runs. The scope
     * shares its enclosing call's rollback scope and operation-completion requirement.
     */
    public fun doWithAutoExecPolicy(policy: AutoExecPolicy, body: OperationBlock)

    /**
     * Throws [AbortTransactionException] to cancel the innermost active restoring block. That block
     * restores its changes and returns normally; an enclosing block can continue. Cancelling an
     * outermost call still permits its completion callback and subsequent finishing work to run, so
     * this does not promise that the whole call leaves the World unchanged.
     */
    public fun abort(): Nothing = throw AbortTransactionException()
  }

  public companion object {
    /**
     * Parses and preprocesses [text] as [P] in this Agent's actor context. Throws
     * [IllegalStateException] if preprocessing produces another element kind.
     */
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
