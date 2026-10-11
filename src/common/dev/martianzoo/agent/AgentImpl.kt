package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.EAGER
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
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.TaskResult
import kotlin.reflect.KClass

/** Implements Actor-contextual parsing, atomic operation coordination, and autoexecution. */
internal class AgentImpl(
    private val world: World,
    private val engine: ActorEngine,
    private val elaborator: PetElaborator,
    private val autoExecLoop: AutoExecLoop,
    private val taskLog: TaskLog,
) : Agent {

  private val autoExecPolicyOverrides = mutableListOf<AutoExecPolicy>()

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

  override var autoExecPolicy: AutoExecPolicy = EAGER
    set(newPolicy) {
      if (newPolicy != field) {
        field = newPolicy
        autoExecAtomically()
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

  override fun sneak(changes: String, fakeCause: Cause?): TaskResult =
      engine.sneak(parseInstructionGroup(changes), fakeCause)

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
      taskLog.capture(actor, "DROP ${tasks.getTaskData(taskId).instruction}") {
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
            throw TaskException(
                "cannot start a manual operation while task $it holds the select-lock"
            )
          }
          addInitialTasks(parseInstructionGroup(initialInstructions))
          continueOperationBody { Adapter().body() }
          engine.requireComplete(allowedPendingTasks)
        },
        validateCompletion = { engine.requireComplete(allowedPendingTasks) },
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

  override fun doTasks(vararg narrowings: String): TaskResult = continueOperation {
    doTasks(*narrowings)
  }

  override fun continueOperation(body: OperationBlock): TaskResult {
    return atomic { continueOperationBody { Adapter().body() } }
  }

  override fun completeOperation(body: OperationBlock): TaskResult {
    return atomic(
        block = {
          continueOperationBody { Adapter().body() }
          engine.requireComplete()
        },
        validateCompletion = { engine.requireComplete() },
    )
  }

  private fun addInitialTasks(initialInstructions: InstructionGroup) {
    engine.addTasks(initialInstructions).forEach { taskId ->
      // Initial routing may assign the task to another Actor before settlement begins.
      if (taskId !in tasks) return@forEach
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

    override fun selectTask(instruction: String) {
      this@AgentImpl.selectTask(instruction)
      autoExecLoop.run()
    }

    override fun selectTask(task: Task) {
      this@AgentImpl.selectTask(task)
      autoExecLoop.run()
    }

    override fun narrowTask(narrowing: String) {
      this@AgentImpl.narrowTask(narrowing)
      autoExecLoop.run()
    }

    override fun doTask(narrowing: String) {
      this@AgentImpl.doTask(narrowing)
      autoExecLoop.run()
    }

    override fun doTasks(vararg narrowings: String) {
      narrowings.forEach { narrowing ->
        val parsed = parseTaskNarrowing(narrowing)
        atomic {
          taskLog.capture(actor, narrowing) {
            engine.doTask(
                parsed.instruction,
                parsed.quantifierOmitted,
                parsed.submittedAsGroup || parsed.instruction is InstructionGroup,
                combineScalars = true,
            )
          }
        }
        autoExecLoop.run()
      }
    }

    override fun doTask(narrowing: String, contextClass: ClassName) {
      this@AgentImpl.doTask(narrowing, contextClass)
      autoExecLoop.run()
    }

    override fun doTask(narrowing: String, taskId: TaskId) {
      this@AgentImpl.doTask(narrowing, taskId)
      autoExecLoop.run()
    }

    override fun tryTask(narrowing: String) {
      this@AgentImpl.tryTask(narrowing)
      autoExecLoop.run()
    }

    override fun tryTask(narrowing: String, taskId: TaskId) {
      this@AgentImpl.tryTask(narrowing, taskId)
      autoExecLoop.run()
    }

    override fun autoExecNow() {
      autoExecLoop.run()
    }

    override fun autoExecNow(policy: AutoExecPolicy) {
      withAutoExecPolicy(policy) { autoExecLoop.run() }
    }
  }

  override fun autoExecNow() = atomic {}

  override fun autoExecNow(policy: AutoExecPolicy): TaskResult =
      withAutoExecPolicy(policy) { atomic {} }

  private inline fun <T> withAutoExecPolicy(policy: AutoExecPolicy, block: () -> T): T {
    autoExecPolicyOverrides.add(policy)
    return try {
      block()
    } finally {
      autoExecPolicyOverrides.removeAt(autoExecPolicyOverrides.lastIndex)
    }
  }

  private fun autoExecAtomically(): TaskResult =
      engine.transact(settle = {}, block = { autoExecLoop.run() })

  // TURNS

  override fun startTurn() = atomic {
    engine
        .addTasks(parseInstructionGroup("NewTurn<$actor>!"))
        .forEach(world.actorEngine(ADMIN)::doTask)
  }

  override fun inTurn(body: OperationBlock): TaskResult {
    return if (tasks.isEmpty()) {
      runOperation("NewTurn", body)
    } else {
      completeOperation(body)
    }
  }

  // GAMES (methods that can't break game-integrity)
  // This layer is only usable if you have a running workflow, so that >0 players always have a
  // task in their queue at any given time

  override fun fillInTask(taskId: TaskId): TaskForm =
      TaskForm(taskId, tasks.getTaskData(taskId).instruction, this)

  internal fun prepareFormNarrowing(taskId: TaskId, narrowing: String): InstructionTree {
    val parsed = parseTaskNarrowing(narrowing)
    return engine.prepareTaskNarrowing(taskId, parsed.instruction, parsed.quantifierOmitted)
  }

  internal fun recheckForm(taskId: TaskId, narrowing: InstructionTree): InstructionTree =
      engine.prepareTaskNarrowing(taskId, narrowing)

  internal fun changeLimit(change: Change): Int? = engine.changeLimit(change)

  internal fun commitForm(taskId: TaskId, narrowing: InstructionTree): TaskResult = atomic {
    taskLog.capture(actor, "CHOOSE $narrowing", formTask = taskId) {
      engine.narrowTask(taskId, narrowing)
    }
  }

  override fun narrowTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    val taskId = tasks.selectedTask() ?: throw TaskException("`$actor` has no selected task")
    val instruction =
        engine.prepareTaskNarrowing(taskId, parsed.instruction, parsed.quantifierOmitted)
    taskLog.capture(actor, "CHOOSE $instruction", formTask = taskId) {
      engine.narrowTask(parsed.instruction, parsed.quantifierOmitted)
    }
  }

  override fun canSelectTask(taskId: TaskId) = engine.canSelectTask(taskId)

  override fun canExecuteTask(taskId: TaskId) = engine.canExecuteTask(taskId)

  override fun selectTask(taskId: TaskId) = atomic {
    taskLog.capture(actor, "CHOOSE ${tasks.getTaskData(taskId).instruction}") {
      engine.selectTask(taskId)
    }
  }

  override fun selectTask(instruction: String) = atomic {
    taskLog.capture(actor, "CHOOSE $instruction") {
      val parsed = parseTaskNarrowing(instruction)
      engine.selectTask(parsed.instruction, parsed.quantifierOmitted)
    }
  }

  override fun selectTask(task: Task) = atomic {
    taskLog.capture(actor, "CHOOSE ${task.instruction}", formTask = task.id) {
      engine.selectTask(task.instruction, taskId = task.id)
    }
  }

  override fun doTask(narrowing: String) = atomic {
    taskLog.capture(actor, "DO $narrowing") {
      val parsed = parseTaskNarrowing(narrowing)
      engine.doTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
      )
    }
  }

  override fun doTask(narrowing: String, contextClass: ClassName) = atomic {
    taskLog.capture(actor, "DO $narrowing") {
      val parsed = parseTaskNarrowing(narrowing)
      engine.doTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
          contextClass = contextClass,
      )
    }
  }

  override fun doTask(narrowing: String, taskId: TaskId) = atomic {
    taskLog.capture(actor, "DO $narrowing") {
      val parsed = parseTaskNarrowing(narrowing)
      engine.doTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
          taskId,
      )
    }
  }

  override fun tryTask(narrowing: String) = atomic {
    taskLog.capture(actor, "DO $narrowing") {
      val parsed = parseTaskNarrowing(narrowing)
      engine.tryTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
      )
    }
  }

  override fun tryTask(narrowing: String, taskId: TaskId) = atomic {
    taskLog.capture(actor, "DO $narrowing") {
      val parsed = parseTaskNarrowing(narrowing)
      engine.tryTask(
          parsed.instruction,
          parsed.quantifierOmitted,
          parsed.submittedAsGroup,
          taskId,
      )
    }
  }

  override fun tryTask(taskId: TaskId) = atomic {
    taskLog.capture(actor, "CHOOSE ${tasks.getTaskData(taskId).instruction}") {
      engine.tryTask(taskId)
    }
  }

  // autoExecNow() and cross-Actor Agent calls can re-enter this call site. Its depth is shared
  // by every Actor in the world so only the true outermost operation drains and reports completion.
  private fun atomic(
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult =
      engine.transact(
          block = block,
          validateCompletion = validateCompletion,
          settle = { autoExecLoop.run() },
      )

  // Direct mutations preserve their legacy behavior of not invoking autoexecution.
  private fun atomicWithoutAutoExec(block: () -> Unit): TaskResult =
      engine.transact(settle = {}, block = block)

  private data class ParsedTaskNarrowing(
      val instruction: InstructionTree,
      val quantifierOmitted: Boolean,
      val submittedAsGroup: Boolean,
  )
}
