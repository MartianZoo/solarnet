package dev.martianzoo.agent

import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
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
 * task retains priority. A nonempty pass through ordinary Admin work must succeed at a task;
 * otherwise the submitted step throws and restores its game changes, even if Player work could
 * unblock it. Deferred Admin work is handled after promotion. Complete enumeration of legal Admin
 * choices is not implemented. Admin retries unfinished analysis after other work progresses; if a
 * pass stalls with an unfinished proof, [IncompleteAnalysisException] fails the submitted step and
 * restores its game changes.
 *
 * A returned [TaskResult] covers every retained change during that call, including Admin handling
 * and configured Player work. A [Throwable] escaping the call restores its game changes, including
 * a successful request followed by failing Admin work. The exception is [doTasks]: it submits
 * individually atomic steps and retains earlier successful steps when a later step throws. See
 * [dev.martianzoo.engine.Timeline.atomic] for the state covered by restoration. [exMachina]
 * deliberately omits task handling; [addTasks] and [dropTask] handle Admin work without choosing
 * Player tasks.
 *
 * Task-instruction strings accept the shorthand `EACH` for this Actor's unique applicable task
 * whose current instruction is an `EACH`. It respects selection, deferral, and any explicit task-ID
 * or context-Class filter. Zero or multiple matches throw [TaskException] without changes. It
 * supplies the existing instruction without choosing its children's targets or amounts; selection
 * only selects it, while execution opens its child tasks. Other text uses ordinary Pets syntax.
 *
 * The current `*Operation` helpers group work across calls: [beginOperation] and
 * [continueOperation] allow tasks to remain, while [runOperation] and [completeOperation] check
 * whether the initiated work has finished. Whether this API grouping should survive is under
 * review.
 */
public interface Agent {

  // READ CALLS

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
   * Narrows this Actor's selected task and evaluates it again without executing component changes.
   * Closed and open instructions remain pending. The surrounding Agent call may separately submit
   * work according to Player policy; Admin handling remains automatic.
   *
   * @param [narrowing] the new instruction tree; may still need narrowing or be a grouped arm
   *   selected from an `OR`; a group stays on the selected task until execution admits its members;
   *   if identical to the current instruction the task remains unchanged; an omitted quantifier
   *   retains a stronger pending quantifier when the Class default would weaken it
   * @throws [TaskException] if this Actor has no selected task
   * @throws [NarrowingException] if [narrowing] does not narrow the selected task's instruction
   */
  public fun narrowTask(narrowing: String): TaskResult

  /**
   * Tries selection and, when the instruction closes, execution of [taskId] in a disposable World.
   * A missing or rejected task yields false; selection needing narrowing can succeed without
   * execution. An assignee transfer does not attempt the receiving Actor's execution. Gameplay
   * rejection and known missing choices yield false. [IncompleteAnalysisException] propagates when
   * a required execution proof cannot finish, including the Admin priority check. Other failures
   * also propagate. Source listeners and existing overlays remain unaffected.
   */
  // TODO(ENGINE_CONTRACT): investigate removing speculative probes from this API. A successful
  // selection can still need narrowing and must not be called a legal complete narrowing.
  public fun canSelectTask(taskId: TaskId): Boolean

  /**
   * Probes whether this actor can select and finish [taskId] now without another choice or assignee
   * transfer. A missing or unfinished task yields false. The probe uses a disposable World; source
   * listeners and existing overlays remain unaffected. Gameplay rejection and known missing choices
   * yield false. [IncompleteAnalysisException] propagates when the engine cannot finish its proof;
   * other failures also propagate.
   */
  // TODO(ENGINE_CONTRACT): investigate whether callers need this execution probe. If retained,
  // define its answer using this instruction's whole transaction, without checking spawned tasks.
  public fun canExecuteTask(taskId: TaskId): Boolean

  /**
   * Selects a pending task and evaluates its instruction against the current World. Closed and open
   * results remain pending for an execution command. A `BY` whose Actor needs no further narrowing
   * or System routing may assign it to another Actor. The call's task handling then runs, so that
   * Actor's policy decides whether the task finishes before this call returns.
   *
   * Groups, `EACH`, and dispatches exposed by selection retain their parent until [doTask] executes
   * it. Selection alone creates no child tasks or continuations.
   *
   * @throws [TaskException] if no task with id [taskId] exists, or if any other task is already
   *   selected
   * @throws [NotNowException] if the selected task cannot execute in the current World
   */
  public fun selectTask(taskId: TaskId): TaskResult

  /**
   * Selects this Actor's sole pending task that is not [deferred][Task.deferred], using
   * [selectTask]'s evaluation and assignee transfer rules, without narrowing it. Throws
   * [TaskException] without changing the World if this Actor has no such task or more than one.
   */
  public fun selectOnlyTask(): TaskResult {
    val id =
        tasks.matching { !it.deferred }.singleOrNull()
            ?: throw TaskException("Expected exactly one pending task that is not deferred")
    return selectTask(id)
  }

  /**
   * Finds the unique pending task compatible with [instruction], selects it, and commits the
   * supplied narrowing. An unchanged instruction only selects. A nontrivial narrowing fails if
   * selection alone transfers the task to another Actor before the narrowing can be applied. Such
   * failures leave the World unchanged. The receiving Actor can narrow the transferred task.
   * Equivalent tasks that differ only by id or originating cause are interchangeable.
   */
  public fun selectTask(instruction: String): TaskResult

  /** Selects [task] by id and submits its instruction as a narrowing of the live task. */
  public fun selectTask(task: Task): TaskResult

  /**
   * Carries out the task matched by the source-level [narrowing] instruction tree. A grouped tree
   * can select a grouped choice and replace the matched task with independent tasks; preprocessing
   * can produce the same replacement, for example when atomizing a multi-step global parameter
   * gain. Explicitly submitted grouped instructions execute as one bundled command; siblings
   * exposed only by preprocessing or evaluation remain ordinary pending tasks. Selects and
   * evaluates the matched task first if necessary. As part of this, executes triggered instructions
   * from *automatic* effects, enqueues tasks for queued effects and any contents of [Task.then],
   * and removes the original task from the game's task queue. Throws an exception if any of this
   * fails.
   *
   * The submitted constraints are intersected with the task: each can supply choices left open by
   * the other. A selected task always wins. Otherwise, exactly one distinct task must intersect,
   * including tasks the submission would not strictly narrow; fully identical tasks remain
   * interchangeable. When the narrowing omits a quantifier and its Class default would weaken the
   * pending task's quantifier, the pending quantifier is retained; an explicitly written quantifier
   * must be compatible with the task's quantifier.
   *
   * With no selected task, a concrete gain/removal (including an atomized group) can instead name
   * the signed total of this Actor's current nondeferred gains/removals of exactly that Type. Agent
   * considers fixed amounts and `/` calculations directly wrapping a change, ignores zero amounts,
   * and requires at least two matches with a shared quantifier. An unspecified amount prevents
   * combining; an explicit submitted quantifier must match. Agent uses all such matches, never
   * searches subsets, and prefers their total even when an individual task has that amount. It
   * executes each matched task separately by ID, gains before removals, preserving its effects and
   * continuation. Newly caused tasks cannot join the total. Any failure rolls back this call. If
   * the total does not match, ordinary task matching applies. Explicit task IDs bypass totals; the
   * context-Class overload restricts the considered tasks to that Class.
   *
   * A submitted zero count retains its Type constraint: `0 Foo` can match `Foo / metric` when the
   * metric evaluates to zero. An unrelated `Ok` alternative does not identify a matching task. A
   * zero count emits no component changes unless it names offsetting nonzero tasks as above.
   *
   * Selection or narrowing may transfer the task to another Actor, including Admin for a System
   * `System` change. Executing NOW consumes the dispatcher and gives its recipient selected work;
   * the recipient supplies remaining choices and executes through its own Agent. Configured
   * autoexecution runs, but a successful assignee transfer may leave that recipient's task
   * selected. Admin failures restore the whole call. SOON and other consequences may leave ordinary
   * pending work.
   *
   * The text names one stage and must not contain `THEN`. A first-stage choice binds shared Type
   * variables and X in its queued continuation. A successful call completes the named stage;
   * executing an `EACH` container completes that stage by creating its child tasks. [doTasks]
   * applies the same rule to each argument and returns only after completing all of them.
   *
   * @throws [NotFullySpecifiedException] if the task still needs narrowing
   * @throws [NotNowException] if the task cannot currently be evaluated or executed
   */
  public fun doTask(narrowing: String): TaskResult

  /**
   * Calls [doTask] for each argument in order and returns their combined changes and surviving new
   * tasks. With no arguments, does nothing and returns an empty result. Each step includes the same
   * task handling as [doTask]; other work may remain pending after the sequence.
   *
   * If a step throws, restores that step and propagates the exception without returning a result.
   * Earlier successful steps remain completed, and later arguments are not attempted. For example,
   * if the third argument fails, the first two remain completed. An exception escaping an enclosing
   * operation, such as [completeOperation] or [continueOperation], restores that whole operation.
   */
  public fun doTasks(vararg narrowings: String): TaskResult

  /** Uses [doTask]'s matching and total rules, restricted to tasks caused by [contextClass]. */
  public fun doTask(narrowing: String, contextClass: ClassName): TaskResult

  /** Carries out [narrowing] against the task identified by [taskId]. */
  public fun doTask(narrowing: String, taskId: TaskId): TaskResult

  /**
   * Selects and executes [taskId], including configured autoexecution after an assignee transfer.
   * As with [doTask], a successful transfer may leave selected work for the receiving Actor. A
   * failed execution restores this call.
   */
  public fun doTask(taskId: TaskId): TaskResult

  /**
   * Attempts [narrowing], then runs configured task handling. Known missing choices or unavailable
   * execution restore the engine attempt. A task left selected, including a transfer to another
   * Actor, also restores the attempt. Subsequent task handling can still execute that task or
   * reject the call. Invalid selection, invalid narrowing, and other failures propagate, including
   * `Die` and failures encountered while handling Admin work. An unavailable `/` metric is handled
   * like unavailable execution: the attempted request leaves no changes.
   * [IncompleteAnalysisException] restores the call and propagates; it is not a known missing
   * choice.
   */
  public fun tryTask(narrowing: String): TaskResult

  /** Tries [narrowing] against the task identified by [taskId]. */
  public fun tryTask(narrowing: String, taskId: TaskId): TaskResult

  /**
   * Attempts to select and execute [taskId], then runs configured task handling. Known missing
   * choices, unavailable execution, or that same task remaining pending restore the engine attempt.
   * A completed NOW dispatcher can leave its newly created recipient task selected. Subsequent task
   * handling can execute pending work or reject the call. Invalid selection and other failures
   * propagate, including `Die` and failures encountered while handling Admin work.
   * [IncompleteAnalysisException] restores the call and propagates; it is not a known missing
   * choice.
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
   * Seeds a legacy call group with [initialInstructions] and requires it to complete after `body`
   * and configured task handling. Other already-pending tasks may remain, but a selected task
   * prevents the new group from starting. Any failure or remaining work created by this group rolls
   * back the whole call.
   *
   * Current limitation: an ordinary nested call checks completion inside its body, but the shared
   * coordinator skips its separate validator after subsequent task handling. The outer call
   * controls that later work and validation.
   */
  // TODO(ENGINE_CONTRACT): investigate removing this legacy call grouping and its completion
  // requirements. Running to quiescence does not establish that all initiated work is complete;
  // these methods additionally infer completion from pending cleanup work across multiple calls.
  public fun runOperation(initialInstructions: String, body: OperationBlock = {}): TaskResult

  /**
   * Starts a resumable group of calls seeded by independent instructions. For example,
   * `beginOperation("X Plant")` may return with the quantity choice still pending; a later call can
   * narrow that task before [completeOperation] finishes the initiated work. The world must have no
   * pending tasks when this call starts.
   */
  public fun beginOperation(initialInstructions: String, body: OperationBlock = {}): TaskResult

  /**
   * Runs `body` and configured autoexecution atomically, allowing tasks to remain pending for a
   * later call. A failure rolls back this call's changes.
   */
  public fun continueOperation(body: OperationBlock = {}): TaskResult

  /**
   * Runs `body` atomically and requires the initiated work to be complete afterward. An incomplete
   * or failed call rolls back this call. Nested calls have the same limitation on final completion
   * validation described by [runOperation].
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
   * Atomically applies fully specified elementary corrections, constructs required parts, and
   * removes dependents. Runs `::` effects and checks every applicable count invariant. Ordinary `:`
   * effects, task handling, and idle cleanup are omitted. Explicit `SOON` dispatches in `::`
   * effects add pending tasks, reported in the result. Explicit System edits are allowed, but no
   * exact MustCleanUp component may increase. Records a replayable correction input and, for a
   * Player, an Audit. Existing tasks retain their current instructions and selection, including
   * calculated amounts.
   */
  public fun exMachina(changes: String): TaskResult

  /**
   * Commands available inside an [OperationBlock]. They act on the enclosing Agent and contribute
   * to the enclosing call's result. Each task selection, narrowing, or submission restores its own
   * changes and automatic follow-up if it throws; [doTasks] applies this separately to each
   * argument. An exception escaping the block restores the enclosing call. The scope's task and
   * reader views reflect changes made earlier in the same block.
   *
   * Explicit [autoExecNow] and [doWithAutoExecPolicy] work shares the enclosing restoring scope;
   * catching its failure inside the block can retain automatic work already completed.
   */
  public interface OperationScope {
    /** This actor's current pending-task view. */
    public val tasks: TaskQueue
    /** Current world view for interpreting the actor's tasks. */
    public val reader: GameReader

    /**
     * Selects and narrows a pending task compatible with [instruction], then runs automatic work.
     * An open result remains selected for [narrowTask]; a closed result may execute or transfer to
     * its assignee. See [Agent.selectTask] for selection and failure rules.
     */
    public fun selectTask(instruction: String)

    /** Selects [task] by id and submits its instruction as a narrowing of the live task. */
    public fun selectTask(task: Task)

    /**
     * Selects this Actor's sole pending task and runs automatic work; see [Agent.selectOnlyTask].
     */
    public fun selectOnlyTask()

    /**
     * Narrows this Actor's selected task and runs automatic work. A System result needing no
     * narrowing passes to Admin for execution. See [Agent.narrowTask].
     */
    public fun narrowTask(narrowing: String)

    /** Executes [narrowing] using [Agent.doTask]'s matching and completion rules. */
    public fun doTask(narrowing: String)

    /** Executes [narrowing] against work caused by a component of [contextClass]. */
    public fun doTask(narrowing: String, contextClass: ClassName)

    /** Executes [narrowing] against the pending task identified by [taskId]. */
    public fun doTask(narrowing: String, taskId: TaskId)

    /** Executes the pending task identified by [taskId], then runs applicable automatic work. */
    public fun doTask(taskId: TaskId)

    /**
     * Calls [doTask] for each argument in order. An empty sequence does nothing. If a step throws,
     * restores that step and stops. Catching the exception inside this scope retains earlier steps;
     * letting it escape restores the enclosing operation. Uses [Agent.doTask]'s matching and
     * combined-total rules.
     */
    public fun doTasks(vararg narrowings: String)

    /** Uses [Agent.tryTask], leaving known missing choices or unavailable work pending. */
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
     * shares its enclosing call's rollback scope and completion requirement.
     */
    public fun doWithAutoExecPolicy(policy: AutoExecPolicy, body: OperationBlock)

    /**
     * Throws [AbortTransactionException] to cancel the innermost active restoring block. That block
     * restores its changes and returns normally; an enclosing block can continue. Cancelling the
     * outermost call skips its remaining task handling and completion validation.
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
