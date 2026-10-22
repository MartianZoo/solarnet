package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.util.Multiset
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.state.RecordedInput.Kind
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult
import kotlin.reflect.KClass

/** Interprets an Actor's input and coordinates the call's engine calls and task handling. */
internal class AgentImpl(
    private val world: World,
    private val engine: ActorEngine,
    private val elaborator: PetElaborator,
    private val autoExecLoop: AutoExecLoop,
    private val taskLog: TaskLog,
    initialPolicy: AutoExecPolicy,
) : Agent {

  private val autoExecPolicyOverrides = mutableListOf<AutoExecPolicy>()
  private var delegatingFromOperationScope = 0

  private fun recordPlayerReturn() {
    if (actor != ADMIN) world.timeline.recordPosition()
  }

  init {
    autoExecLoop.register(engine.actor) { autoExecPolicyOverrides.lastOrNull() ?: autoExecPolicy }
  }

  override val actor: Actor
    get() = engine.actor

  override val reader: GameReader
    get() = engine.reader

  override val tasks: TaskQueue
    get() = engine.tasks

  private val allTasks: TaskQueue
    get() = world.tasks

  // TODO(ENGINE_CONTRACT): proposed Admin handling is mandatory. Admin's own configured or
  // overridden policy can currently disable it, and Player policies can hide its failure.
  override var autoExecPolicy: AutoExecPolicy = initialPolicy
    set(newPolicy) {
      if (newPolicy != field) {
        field = newPolicy
        if (newPolicy != AutoExecPolicy.NONE) {
          autoExecAtomically()
          recordPlayerReturn()
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

  private fun parseTaskNarrowing(text: String): ParsedTaskNarrowing {
    val parsed = Parsing.parse<InstructionTree>(text)
    return ParsedTaskNarrowing(
        elaborator.elaborateInput(parsed, actor as? Player),
        quantifierOmitted = parsed is Change && parsed.quantifier == null,
        submittedAsGroup = parsed is InstructionGroup,
    )
  }

  private fun parseInstructionGroup(text: String): InstructionGroup =
      InstructionGroup.of(parse<InstructionTree>(text))

  // CHANGES

  // TODO(ENGINE_CONTRACT): decide whether correction calls may deliberately omit required Admin
  // handling. This path currently returns without checking for eligible Admin tasks.
  override fun sneak(changes: String, fakeCause: Cause?): TaskResult =
      world.timeline
          .atomic {
            engine.sneak(parseInstructionGroup(changes), fakeCause)
            world.agentTransactionGuard?.validate()
          }
          .also { recordPlayerReturn() }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> {
    var added = emptyList<TaskId>()
    atomicWithoutAutoExec {
      added = engine.addTasks(parseInstructionGroup(instruction), firstCause)
    }
    return added
  }

  override fun dropTask(taskId: TaskId): TaskRemovedEvent {
    lateinit var removed: TaskRemovedEvent
    atomicWithoutAutoExec {
      taskLog.capture(
          actor,
          tasks.getTaskData(taskId).instruction.toString(),
          kind = Kind.DROP,
          taskId = taskId,
      ) {
        removed = engine.dropTask(taskId)
      }
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
            throw TaskException("cannot start a manual operation while task $it is selected")
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

  override fun doTasks(vararg narrowings: String): TaskResult = atomic {
    continueOperationBody { Adapter().performTasks(narrowings) }
  }

  override fun continueOperation(body: OperationBlock): TaskResult {
    return atomic { continueOperationBody { Adapter().body() } }
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
      // Initial routing may assign the task to another Actor before task handling begins, and a
      // dispatch admits work that may wait.
      if (taskId !in tasks || tasks.getTaskData(taskId).deferred) return@forEach
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

    private inline fun <T> delegate(block: () -> T): T {
      delegatingFromOperationScope++
      return try {
        block()
      } finally {
        delegatingFromOperationScope--
      }
    }

    private inline fun <T> returnedCall(block: () -> T): T {
      val result = delegate(block)
      autoExecLoop.run()
      recordPlayerReturn()
      return result
    }

    override fun selectTask(instruction: String) {
      returnedCall { this@AgentImpl.selectTask(instruction) }
    }

    override fun selectTask(task: Task) {
      returnedCall { this@AgentImpl.selectTask(task) }
    }

    override fun selectOnlyTask(x: Int?) {
      returnedCall { this@AgentImpl.selectOnlyTask(x) }
    }

    override fun narrowTask(narrowing: String) {
      returnedCall { this@AgentImpl.narrowTask(narrowing) }
    }

    override fun doTask(narrowing: String) {
      returnedCall { this@AgentImpl.doTask(narrowing) }
    }

    fun performTasks(narrowings: Array<out String>) {
      narrowings.forEach { narrowing ->
        val parsed = parseTaskNarrowing(narrowing)
        delegate {
          decide {
            taskLog.capture(actor, narrowing) {
              engine.doTask(
                  parsed.instruction,
                  parsed.quantifierOmitted,
                  parsed.submittedAsGroup || parsed.instruction is InstructionGroup,
                  combineScalars = true,
              )
            }
            finishTaskHandoff()
          }
        }
        autoExecLoop.run()
      }
    }

    override fun doTasks(vararg narrowings: String) {
      performTasks(narrowings)
      recordPlayerReturn()
    }

    override fun doTask(narrowing: String, contextClass: ClassName) {
      returnedCall { this@AgentImpl.doTask(narrowing, contextClass) }
    }

    override fun doTask(narrowing: String, taskId: TaskId) {
      returnedCall { this@AgentImpl.doTask(narrowing, taskId) }
    }

    override fun tryTask(narrowing: String) {
      returnedCall { this@AgentImpl.tryTask(narrowing) }
    }

    override fun tryTask(narrowing: String, taskId: TaskId) {
      returnedCall { this@AgentImpl.tryTask(narrowing, taskId) }
    }

    override fun autoExecNow() {
      autoExecLoop.run()
      recordPlayerReturn()
    }

    override fun autoExecNow(policy: AutoExecPolicy) {
      withAutoExecPolicyOverride(policy) { autoExecLoop.run() }
      recordPlayerReturn()
    }

    override fun doWithAutoExecPolicy(policy: AutoExecPolicy, body: OperationBlock) {
      withAutoExecPolicyOverride(policy) {
        autoExecLoop.run()
        body(this@Adapter)
        autoExecLoop.run()
      }
      recordPlayerReturn()
    }
  }

  override fun autoExecNow() = atomic {}

  override fun autoExecNow(policy: AutoExecPolicy): TaskResult =
      withAutoExecPolicyOverride(policy) { atomic {} }

  private inline fun <T> withAutoExecPolicyOverride(policy: AutoExecPolicy, block: () -> T): T {
    autoExecPolicyOverrides.add(policy)
    return try {
      block()
    } finally {
      autoExecPolicyOverrides.removeAt(autoExecPolicyOverrides.lastIndex)
    }
  }

  private fun autoExecAtomically(): TaskResult =
      engine.transact(
          afterChanges = { world.agentTransactionGuard?.validate() },
          block = { autoExecLoop.run() },
      )

  override fun fillInTask(taskId: TaskId): TaskForm =
      TaskForm(taskId, tasks.getTaskData(taskId).instruction, this)

  internal fun prepareFormNarrowing(taskId: TaskId, narrowing: String): InstructionTree {
    val parsed = parseTaskNarrowing(narrowing)
    return engine.prepareTaskNarrowing(taskId, parsed.instruction, parsed.quantifierOmitted)
  }

  internal fun recheckForm(taskId: TaskId, narrowing: InstructionTree): InstructionTree =
      engine.prepareTaskNarrowing(taskId, narrowing)

  internal fun changeLimit(change: Change): Int? = engine.changeLimit(change)

  internal fun commitForm(taskId: TaskId, narrowing: InstructionTree): TaskResult = decide {
    taskLog.capture(actor, narrowing.toString(), formTask = taskId, kind = Kind.CHOOSE) {
      engine.narrowTask(taskId, narrowing)
    }
  }

  override fun narrowTask(narrowing: String) = decide {
    val parsed = parseTaskNarrowing(narrowing)
    val taskId = tasks.selectedTask() ?: throw TaskException("`$actor` has no selected task")
    val instruction =
        engine.prepareTaskNarrowing(taskId, parsed.instruction, parsed.quantifierOmitted)
    taskLog.capture(actor, instruction.toString(), formTask = taskId, kind = Kind.CHOOSE) {
      engine.narrowTask(parsed.instruction, parsed.quantifierOmitted)
    }
  }

  override fun canSelectTask(taskId: TaskId) = engine.canSelectTask(taskId)

  override fun canExecuteTask(taskId: TaskId) = engine.canExecuteTask(taskId)

  override fun selectTask(taskId: TaskId) = decide {
    taskLog.capture(
        actor,
        tasks.getTaskData(taskId).instruction.toString(),
        kind = Kind.CHOOSE,
        taskId = taskId,
    ) {
      engine.selectTask(taskId)
    }
  }

  override fun selectTask(instruction: String) = decide {
    taskLog.capture(actor, instruction, kind = Kind.CHOOSE) {
      val parsed = parseTaskNarrowing(instruction)
      engine.selectTask(parsed.instruction, parsed.quantifierOmitted)
    }
  }

  override fun selectTask(task: Task) = decide {
    taskLog.capture(
        actor,
        task.instruction.toString(),
        formTask = task.id,
        kind = Kind.CHOOSE,
        taskId = task.id,
    ) {
      engine.selectTask(task.instruction, taskId = task.id)
    }
  }

  override fun doTask(narrowing: String) = decide {
    taskLog.capture(actor, narrowing, kind = Kind.DO_TASK) {
      val parsed = parseTaskNarrowing(narrowing)
      engine.doTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
      )
    }
    finishTaskHandoff()
  }

  override fun doTask(narrowing: String, contextClass: ClassName) = decide {
    taskLog.capture(actor, narrowing, kind = Kind.DO_TASK) {
      val parsed = parseTaskNarrowing(narrowing)
      engine.doTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
          contextClass = contextClass,
      )
    }
    finishTaskHandoff()
  }

  override fun doTask(narrowing: String, taskId: TaskId) = decide {
    taskLog.capture(actor, narrowing, kind = Kind.DO_TASK, taskId = taskId) {
      val parsed = parseTaskNarrowing(narrowing)
      engine.doTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
          taskId,
      )
    }
    finishTaskHandoff()
  }

  override fun doTask(taskId: TaskId) = decide {
    taskLog.capture(
        actor,
        tasks.getTaskData(taskId).instruction.toString(),
        kind = Kind.DO_TASK_ID,
        taskId = taskId,
    ) {
      engine.doTask(taskId)
    }
    finishTaskHandoff()
  }

  override fun tryTask(narrowing: String) = decide {
    taskLog.capture(actor, narrowing, kind = Kind.TRY_TASK) {
      val parsed = parseTaskNarrowing(narrowing)
      engine.tryTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
      )
    }
  }

  override fun tryTask(narrowing: String, taskId: TaskId) = decide {
    taskLog.capture(actor, narrowing, kind = Kind.TRY_TASK, taskId = taskId) {
      val parsed = parseTaskNarrowing(narrowing)
      engine.tryTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
          taskId,
      )
    }
  }

  override fun tryTask(taskId: TaskId) = decide {
    taskLog.capture(
        actor,
        tasks.getTaskData(taskId).instruction.toString(),
        kind = Kind.TRY_TASK_ID,
        taskId = taskId,
    ) {
      engine.tryTask(taskId)
    }
  }

  /** Runs a submitted decision and task handling within the enclosing call. */
  private fun decide(block: () -> Unit): TaskResult = atomic {
    markDecision()
    block()
  }

  private fun finishTaskHandoff() {
    val pending = world.tasks.selectedTask() ?: return
    autoExecLoop.run()
    if (pending in world.tasks) {
      throw TaskException("task $pending did not finish after its Actor handoff")
    }
  }

  private fun markDecision() {
    autoExecLoop.lastDecision = world.timeline.checkpoint()
  }

  private fun requireComplete(allowedPendingTasks: Set<TaskId> = emptySet()) =
      engine.requireComplete(allowedPendingTasks, autoExecLoop.lastDecision)

  // Agent calls can re-enter this site. Shared depth currently makes the outermost call handle
  // tasks and report completion, even when several Actor engine calls happen inside it.
  // TODO(ENGINE_CONTRACT): run mandatory Admin work to quiescence regardless of Player policy.
  // The returned TaskResult must include all retained changes through the end of the call.
  private fun atomic(
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult =
      engine
          .transact(
              block = block,
              validateCompletion = validateCompletion,
              afterChanges = {
                autoExecLoop.run()
                world.agentTransactionGuard?.validate()
              },
          )
          .also {
            if (delegatingFromOperationScope == 0) recordPlayerReturn()
          }

  // TODO(ENGINE_CONTRACT): task additions and removals are changes too. Handle eligible Admin work
  // before these calls return; the current path deliberately skips all task handling.
  private fun atomicWithoutAutoExec(block: () -> Unit): TaskResult =
      engine
          .transact(afterChanges = { world.agentTransactionGuard?.validate() }, block = block)
          .also {
            if (delegatingFromOperationScope == 0) recordPlayerReturn()
          }

  private data class ParsedTaskNarrowing(
      val instruction: InstructionTree,
      val quantifierOmitted: Boolean,
      val submittedAsGroup: Boolean,
  )
}
