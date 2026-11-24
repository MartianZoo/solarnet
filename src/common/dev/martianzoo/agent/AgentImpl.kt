package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.Exceptions.AbortTransactionException
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Deferral.FINALLY
import dev.martianzoo.pets.ast.Deferral.NOW
import dev.martianzoo.pets.ast.Deferral.SOON
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Dispatch
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.util.Multiset
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult
import kotlin.reflect.KClass

/** Interprets an Actor's input and coordinates the call's engine calls and task handling. */
internal class AgentImpl(
    private val agents: Agents,
    private val engine: ActorEngine,
    private val elaborator: PetElaborator,
    private val autoExecLoop: AutoExecLoop,
    private val validateApplication: () -> Unit,
    initialPolicy: AutoExecPolicy,
) : Agent {

  private val world: World
    get() = agents.world

  override fun doRoutine(
      name: String,
      vararg arguments: String,
      releasedBy: Agent?,
  ): TaskResult {
    val routine = requireNotNull(agents.routines[name]) { "Unknown routine: $name" }
    return world.timeline.atomic {
      routine.perform(agents, this, arguments.toList(), releasedBy)
    }
  }

  private val autoExecPolicyOverrides = mutableListOf<AutoExecPolicy>()

  init {
    autoExecLoop.register(engine.actor) { autoExecPolicyOverrides.lastOrNull() ?: autoExecPolicy }
  }

  override val actor: Actor
    get() = engine.actor

  override val reader: GameReader
    get() = world.reader

  override val tasks: TaskQueue
    get() = engine.tasks

  private val allTasks: TaskQueue
    get() = world.tasks

  override var autoExecPolicy: AutoExecPolicy =
      if (actor == ADMIN) AutoExecPolicy.EAGER else initialPolicy
    set(newPolicy) {
      require(actor != ADMIN || newPolicy == AutoExecPolicy.EAGER) {
        "Admin always uses EAGER handling"
      }
      if (newPolicy != field) {
        field = newPolicy
        if (newPolicy != AutoExecPolicy.NONE) {
          autoExecAtomically()
        }
      }
    }

  // READ-ONLY

  override fun has(requirement: String) = reader.has(parse(requirement))

  override fun count(metric: String) =
      reader.count(
          elaborator.elaborateMetricInput(
              Parsing.parse(metric),
              actor.expression,
              actor as? Player,
          )
      )

  override fun list(type: String): Multiset<Expression> =
      reader.getComponents(reader.resolve(parse(type))).map { it.expression }

  override fun resolve(expression: String) = reader.resolve(parse(expression))

  override fun parseAs(type: KClass<out PetElement>, text: String): PetElement =
      elaborator.elaborateInput(Parsing.parse(type, text), actor as? Player)

  private fun parseTaskNarrowing(
      text: String,
      contextClass: ClassName? = null,
      taskId: TaskId? = null,
  ): ParsedTaskNarrowing {
    if (text.trim() == "EACH") {
      val selected = allTasks.selectedTask()
      val task =
          tasks
              .extract { it }
              .singleOrNull {
                it.deferral == allTasks.earliestDeferral &&
                    it.instruction is Each &&
                    (selected == null || it.id == selected) &&
                    (taskId == null || it.id == taskId) &&
                    (contextClass == null || it.cause?.context?.className == contextClass)
              } ?: throw TaskException("Expected exactly one applicable EACH task for $actor")
      return ParsedTaskNarrowing(task.instruction, quantifierOmitted = false, taskId = task.id)
    }
    val parsed = Parsing.parse<InstructionTree>(text)
    return ParsedTaskNarrowing(
        elaborator.elaborateInput(parsed, actor as? Player),
        quantifierOmitted = parsed is Change && parsed.quantifier == null,
    )
  }

  private fun parseInstructionGroup(text: String): InstructionGroup =
      InstructionGroup.of(parse<InstructionTree>(text))

  // CHANGES

  override fun exMachina(changes: String): TaskResult =
      world.timeline.atomic {
        val unfinished = list("MustCleanUp").entries.associate { it.key to it.value }
        val correction = parseInstructionGroup(changes)
        val audited =
            if (actor is Player)
                InstructionGroup.of(listOf(parseInstructionGroup("Audit<$actor>"), correction))
            else correction
        engine.exMachina(audited)
        validateApplication()
        if (list("MustCleanUp").entries.any { (type, count) -> count > (unfinished[type] ?: 0) }) {
          throw NotNowException("exMachina cannot leave new unfinished work")
        }
      }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> {
    var added = emptyList<TaskId>()
    queueEdit {
      added = engine.addTasks(parseInstructionGroup(instruction), firstCause)
    }
    return added
  }

  override fun dropTask(taskId: TaskId): TaskRemovedEvent {
    lateinit var removed: TaskRemovedEvent
    queueEdit {
      removed = engine.dropTask(taskId)
    }
    return removed
  }

  // OPERATIONS

  override fun runOperation(initialInstructions: String, body: OperationBlock): TaskResult {
    var allowedPendingTasks = emptySet<TaskId>()
    return atomic(
        block = {
          allowedPendingTasks = allTasks.ids()
          allTasks.selectedTask()?.let {
            throw TaskException("cannot start a new call group while task $it is selected")
          }
          markDecision()
          addInitialTasks(parseInstructionGroup(initialInstructions))
          continueOperationBody { Adapter().body() }
          requireComplete(allowedPendingTasks)
        },
        validateCompletion = { requireComplete(allowedPendingTasks) },
    )
  }

  override fun beginOperation(initialInstructions: String, body: OperationBlock): TaskResult {
    return atomic {
      if (!allTasks.isEmpty()) {
        throw TaskException("pending tasks:\n${allTasks.extract { it }.joinToString("\n")}")
      }
      addInitialTasks(parseInstructionGroup(initialInstructions))
      continueOperationBody { Adapter().body() }
    }
  }

  override fun doTasks(vararg narrowings: String): TaskResult {
    val before = world.events.nextOrdinal
    val pendingBefore = allTasks.ids()
    narrowings.forEach { doTask(it) }
    return TaskResult(world.events.changesSince(before), allTasks.ids() - pendingBefore)
  }

  override fun continueOperation(body: OperationBlock): TaskResult {
    return atomic { continueOperationBody { Adapter().body() } }
  }

  override fun doWithAutoExecPolicy(policy: AutoExecPolicy, body: OperationBlock): TaskResult =
      withAutoExecPolicyOverride(policy) {
        atomic { continueOperationBody { Adapter().body() } }
      }

  override fun completeOperation(body: OperationBlock): TaskResult {
    return atomic(
        block = {
          markDecision()
          continueOperationBody { Adapter().body() }
          requireComplete()
        },
        validateCompletion = { requireComplete() },
    )
  }

  private fun addInitialTasks(initialInstructions: InstructionGroup) {
    engine.addTasks(initialInstructions).forEach { taskId ->
      autoExecLoop.runAdmin()
      // Initial routing may assign the task to another Actor before task handling begins, and a
      // dispatch admits work that may wait.
      if (taskId !in tasks || tasks.getTaskData(taskId).deferral > allTasks.earliestDeferral)
          return@forEach
      try {
        engine.doTask(taskId)
      } catch (_: NotFullySpecifiedException) {
        // Initial abstract work remains pending for the operation body to narrow.
      }
    }
  }

  private inline fun continueOperationBody(body: () -> Unit) {
    autoExecLoop.run()
    body()
    autoExecLoop.run()
  }

  private inner class Adapter : OperationScope {
    override val tasks = this@AgentImpl.tasks

    override val reader = this@AgentImpl.reader

    private fun returnedCall(block: () -> Unit) {
      atomic {
        block()
        autoExecLoop.run()
      }
    }

    override fun selectTask(instruction: String) {
      returnedCall { this@AgentImpl.selectTask(instruction) }
    }

    override fun selectTask(task: Task) {
      returnedCall { this@AgentImpl.selectTask(task) }
    }

    override fun selectOnlyTask() {
      returnedCall { this@AgentImpl.selectOnlyTask() }
    }

    override fun narrowTask(narrowing: String) {
      returnedCall { this@AgentImpl.narrowTask(narrowing) }
    }

    override fun doTask(narrowing: String) {
      returnedCall { this@AgentImpl.doTask(narrowing) }
    }

    override fun doTasks(vararg narrowings: String) {
      narrowings.forEach { doTask(it) }
    }

    override fun doTask(narrowing: String, contextClass: ClassName) {
      returnedCall { this@AgentImpl.doTask(narrowing, contextClass) }
    }

    override fun doTask(narrowing: String, taskId: TaskId) {
      returnedCall { this@AgentImpl.doTask(narrowing, taskId) }
    }

    override fun doTask(taskId: TaskId) {
      returnedCall { this@AgentImpl.doTask(taskId) }
    }

    override fun tryTask(narrowing: String) {
      returnedCall { this@AgentImpl.tryTask(narrowing) }
    }

    override fun tryTask(narrowing: String, taskId: TaskId) {
      returnedCall { this@AgentImpl.tryTask(narrowing, taskId) }
    }

    override fun autoExecNow() {
      autoExecLoop.run()
    }

    override fun autoExecNow(policy: AutoExecPolicy) {
      withAutoExecPolicyOverride(policy) { autoExecLoop.run() }
    }

    override fun doWithAutoExecPolicy(policy: AutoExecPolicy, body: OperationBlock) {
      withAutoExecPolicyOverride(policy) {
        autoExecLoop.run()
        body(this@Adapter)
        autoExecLoop.run()
      }
    }
  }

  override fun autoExecNow() = atomic {}

  override fun autoExecNow(policy: AutoExecPolicy): TaskResult =
      withAutoExecPolicyOverride(policy) { atomic {} }

  private inline fun <T> withAutoExecPolicyOverride(policy: AutoExecPolicy, block: () -> T): T {
    require(actor != ADMIN || policy == AutoExecPolicy.EAGER) { "Admin always uses EAGER handling" }
    autoExecPolicyOverrides.add(policy)
    return try {
      block()
    } finally {
      autoExecPolicyOverrides.removeAt(autoExecPolicyOverrides.lastIndex)
    }
  }

  private fun autoExecAtomically(): TaskResult =
      engine.transact(
          afterChanges = { validateApplication() },
          block = { autoExecLoop.run() },
      )

  override fun fillInTask(taskId: TaskId): TaskForm =
      TaskForm(taskId, tasks.getTaskData(taskId).instruction, this)

  internal fun prepareFormNarrowing(taskId: TaskId, narrowing: String): InstructionTree {
    val parsed = parseTaskNarrowing(narrowing, taskId = taskId)
    return engine.prepareTaskNarrowing(taskId, parsed.instruction, parsed.quantifierOmitted)
  }

  internal fun recheckForm(taskId: TaskId, narrowing: InstructionTree): InstructionTree =
      engine.prepareTaskNarrowing(taskId, narrowing)

  internal fun changeLimit(change: Change): Int? = engine.changeLimit(change)

  internal fun commitForm(taskId: TaskId, narrowing: InstructionTree): TaskResult = decide {
    engine.narrowTask(taskId, narrowing)
  }

  override fun narrowTask(narrowing: String) = decide {
    val parsed = parseTaskNarrowing(narrowing)
    val taskId = tasks.selectedTask() ?: throw TaskException("`$actor` has no selected task")
    engine.narrowTask(taskId, parsed.instruction, parsed.quantifierOmitted)
  }

  override fun canSelectTask(taskId: TaskId) = engine.canSelectTask(taskId)

  override fun selectTask(taskId: TaskId) = decide {
    engine.selectTask(taskId)
  }

  override fun selectTask(instruction: String) = decide {
    val parsed = parseTaskNarrowing(instruction)
    engine.selectTask(parsed.instruction, parsed.quantifierOmitted, parsed.taskId)
  }

  override fun selectTask(task: Task) = decide {
    engine.selectTask(task.instruction, taskId = task.id)
  }

  private fun executeInput(parsed: ParsedTaskNarrowing, contextClass: ClassName? = null) {
    if (allTasks.selectedTask() == null && combineAmounts(parsed, contextClass)) return
    engine.doTask(parsed.instruction, parsed.quantifierOmitted, parsed.taskId, contextClass)
  }

  private fun combineAmounts(parsed: ParsedTaskNarrowing, contextClass: ClassName?): Boolean {
    val matches =
        engine.taskMatcher.matchingAmounts(
            parsed.instruction,
            parsed.quantifierOmitted,
            contextClass,
            tasks.extract { it to it.instruction },
        ) ?: return false
    matches.forEach { (task, change) ->
      engine.doTask(change, taskId = task.id)
      autoExecLoop.runAdmin()
    }
    return true
  }

  override fun doTask(narrowing: String, releasedBy: Agent): TaskResult = decide {
    require(releasedBy is AgentImpl && releasedBy.autoExecLoop === autoExecLoop) {
      "The releasing Agent must belong to the same session"
    }
    require(actor is Player && releasedBy.actor is Player && actor != releasedBy.actor)
    releasedBy.withAutoExecPolicyOverride(AutoExecPolicy.NONE) {
      withAutoExecPolicyOverride(AutoExecPolicy.NONE) {
        if (allTasks.selectedTask() != null) {
          doTask(narrowing)
        } else {
          val releases =
              releasedBy.tasks
                  .extract { task ->
                    val dispatch = task.instruction as? Dispatch
                    if (
                        task.deferral == allTasks.earliestDeferral &&
                            dispatch?.deferral == NOW &&
                            dispatch.actor == actor.expression
                    )
                        task to dispatch.body
                    else null
                  }
                  .filterNotNull()
          if (releases.isEmpty()) {
            doTask(narrowing)
          } else {
            val parsed = parseTaskNarrowing(narrowing)
            val matches =
                engine.taskMatcher.matchingAmounts(
                    parsed.instruction,
                    parsed.quantifierOmitted,
                    null,
                    tasks.extract { it to it.instruction } + releases,
                    requireWholeTotal = true,
                )
            if (matches == null) {
              val task =
                  engine.taskMatcher
                      .match(
                          actor,
                          parsed.instruction,
                          parsed.quantifierOmitted,
                          releases = releases,
                      )
                      ?.task ?: throw TaskException("no matching task for `$narrowing`")
              if (task.assignee != actor) releasedBy.doTask(task.id)
              doTask(narrowing)
            } else {
              matches.forEach { (task, change) ->
                if (task.assignee == actor) {
                  doTask(change.toString(), task.id)
                } else {
                  releasedBy.doTask("NOW $actor { $change }", task.id)
                  doTask(change.toString())
                }
              }
            }
          }
        }
      }
    }
  }

  override fun doTask(narrowing: String) = decide {
    val parsed = parseTaskNarrowing(narrowing)
    executeInput(parsed)
    finishTaskHandoff()
  }

  override fun doTask(narrowing: String, contextClass: ClassName) = decide {
    val parsed = parseTaskNarrowing(narrowing, contextClass = contextClass)
    executeInput(parsed, contextClass)
    finishTaskHandoff()
  }

  override fun doTask(narrowing: String, taskId: TaskId) = decide {
    val parsed = parseTaskNarrowing(narrowing, taskId = taskId)
    engine.doTask(
        parsed.instruction,
        parsed.quantifierOmitted,
        taskId,
    )
    finishTaskHandoff()
  }

  override fun doTask(taskId: TaskId) = decide {
    engine.doTask(taskId)
    finishTaskHandoff()
  }

  override fun tryTask(narrowing: String) = decide {
    val parsed = parseTaskNarrowing(narrowing)
    tryExecution {
      engine.doTask(parsed.instruction, parsed.quantifierOmitted, parsed.taskId)
      if (allTasks.selectedTask() != null) throw AbortTransactionException()
    }
  }

  override fun tryTask(narrowing: String, taskId: TaskId) = decide {
    val parsed = parseTaskNarrowing(narrowing, taskId = taskId)
    tryExecution {
      engine.doTask(parsed.instruction, parsed.quantifierOmitted, taskId)
      if (allTasks.selectedTask() != null) throw AbortTransactionException()
    }
  }

  override fun tryTask(taskId: TaskId) = decide {
    tryExecution {
      engine.doTask(taskId)
      if (taskId in allTasks) throw AbortTransactionException()
    }
  }

  private fun tryExecution(block: () -> Unit) {
    try {
      world.timeline.atomic(block)
    } catch (e: IncompleteAnalysisException) {
      throw e
    } catch (_: NotFullySpecifiedException) {
      // A known missing choice leaves the request pending; unfinished analysis propagates.
    } catch (_: NotNowException) {
      // Currently unavailable work leaves the attempted task and its effects unchanged.
    }
  }

  /** Runs a submitted decision and task handling within the enclosing call. */
  private fun decide(block: () -> Unit): TaskResult = atomic {
    autoExecLoop.runAdmin()
    markDecision()
    block()
    autoExecLoop.runAdmin()
  }

  private fun finishTaskHandoff() {
    if (world.tasks.selectedTask() == null) return
    autoExecLoop.run()
  }

  private fun markDecision() {
    autoExecLoop.lastDecision = world.events.nextOrdinal
  }

  /**
   * Checks pending work after this Agent call. Once `FINALLY` work begins, later tasks belong to
   * subsequent work; cleanup is checked at the first such selection, not at the current World
   * state. Whether that selection is always the right completion point remains open.
   */
  private fun requireComplete(allowedPendingTasks: Set<TaskId> = emptySet()) {
    val pending = allTasks.extract { it }.filter { it.id !in allowedPendingTasks }
    val workOver = autoExecLoop.lastDecision?.let(::finalWorkBegan)
    if (pending.isNotEmpty() && workOver == null) {
      if (pending.any { it.instruction.needsNarrowing(reader) }) {
        throw NotFullySpecifiedException(
            "pending tasks need narrowing:\n${pending.joinToString("\n")}"
        )
      }
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
    val cleanupLeft =
        if (pending.isEmpty() || workOver == null)
            reader.count(reader.resolve(MUST_CLEAN_UP.expression)) > 0
        else cleanupComponentsAt(workOver) > 0
    if (cleanupLeft) {
      throw DeadEndException(
          "components requiring cleanup remained after this call: " +
              reader.getComponents(MUST_CLEAN_UP.expression).elements
      )
    }
  }

  /** The event ordinal at which the World's `FINALLY` work first began since [eventCursor]. */
  private fun finalWorkBegan(eventCursor: Int): Int? =
      world.events
          .entriesSince(eventCursor)
          .firstOrNull {
            it is TaskEditedEvent && it.oldTask.deferral == FINALLY && it.task.deferral == SOON
          }
          ?.ordinal

  /** How many cleanup components existed at [eventCursor]: those present now, less later change. */
  private fun cleanupComponentsAt(eventCursor: Int): Int {
    val mustCleanUp = reader.classTable.getClass(MUST_CLEAN_UP)
    fun cleanup(component: Component?) =
        component?.type?.rootClass?.isSubtypeOf(mustCleanUp) == true
    val changeSince =
        world.events.changesSince(eventCursor).sumOf {
          (if (cleanup(it.change.gaining)) it.change.count else 0) -
              (if (cleanup(it.change.removing)) it.change.count else 0)
        }
    return reader.count(reader.resolve(MUST_CLEAN_UP.expression)) - changeSince
  }

  // Agent calls can re-enter this site. Shared depth currently makes the outermost call handle
  // tasks and report completion, even when several Actor engine calls happen inside it.
  private fun atomic(
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult =
      engine.transact(
          block = block,
          validateCompletion = validateCompletion,
          afterChanges = {
            autoExecLoop.run()
            validateApplication()
          },
      )

  // Queue edits handle mandatory Admin work without choosing Player tasks.
  private fun queueEdit(block: () -> Unit): TaskResult =
      engine.transact(
          afterChanges = {
            autoExecLoop.runAdmin()
            validateApplication()
          },
          block = {
            autoExecLoop.runAdmin()
            block()
            autoExecLoop.runAdmin()
          },
      )

  private data class ParsedTaskNarrowing(
      val instruction: InstructionTree,
      val quantifierOmitted: Boolean,
      val taskId: TaskId? = null,
  )
}
