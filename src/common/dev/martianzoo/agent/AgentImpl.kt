package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.engine.ActorEngine
import dev.martianzoo.engine.TaskQueue
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.util.Multiset
import kotlin.reflect.KClass

/** Implements Actor-contextual parsing, atomic operation coordination, and autoexecution. */
internal class AgentImpl(
    private val engine: ActorEngine,
    private val elaborator: PetElaborator,
    private val autoExecLoop: AutoExecLoop,
) : Agent {

  override val actor: Actor
    get() = engine.actor

  override val reader: GameReader
    get() = engine.reader

  override val tasks: TaskQueue
    get() = engine.tasks

  private val allTasks: TaskQueue
    get() = engine.allTasks

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
        intensityOmitted = parsed is Change && parsed.intensity == null,
        submittedAsGroup = parsed is InstructionGroup,
    )
  }

  private fun parseInstructionGroup(text: String): InstructionGroup =
      InstructionGroup.of(parse<InstructionTree>(text))

  // CHANGES

  override fun sneak(changes: String, fakeCause: Cause?): TaskResult = atomicWithoutAutoExec {
    engine.sneak(parseInstructionGroup(changes), fakeCause)
  }

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
    atomicWithoutAutoExec { removed = engine.dropTask(taskId) }
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
                "can't start a manual operation while task $it holds the select-lock"
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
      try {
        engine.doTask(taskId)
      } catch (_: AbstractException) {
        // Initial abstract work remains pending for the operation body to narrow.
      }
    }
  }

  private inline fun continueOperationBody(body: () -> Unit) {
    autoExecLoop.run(actor, autoExecPolicy)
    body()
    autoExecLoop.run(actor, autoExecPolicy)
  }

  private inner class Adapter : OperationScope {
    override val tasks = this@AgentImpl.tasks

    override val reader = this@AgentImpl.reader

    override fun doTask(narrowing: String) {
      this@AgentImpl.doTask(narrowing)
      autoExecLoop.run(actor, autoExecPolicy)
    }

    override fun doTask(narrowing: String, taskId: TaskId) {
      this@AgentImpl.doTask(narrowing, taskId)
      autoExecLoop.run(actor, autoExecPolicy)
    }

    override fun tryTask(narrowing: String) {
      this@AgentImpl.tryTask(narrowing)
      autoExecLoop.run(actor, autoExecPolicy)
    }

    override fun tryTask(narrowing: String, taskId: TaskId) {
      this@AgentImpl.tryTask(narrowing, taskId)
      autoExecLoop.run(actor, autoExecPolicy)
    }

    override fun autoExecNow() {
      autoExecLoop.run(actor, autoExecPolicy)
    }
  }

  override fun autoExecNow() = atomic {}

  private fun autoExecAtomically(): TaskResult =
      engine.transact(settle = {}, block = { autoExecLoop.run(actor, autoExecPolicy) })

  // TURNS

  override fun startTurn() = atomic {
    engine.addTasks(parseInstructionGroup("NewTurn<$actor>!")).forEach(engine::doTask)
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

  override fun narrowTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    engine.narrowTask(parsed.instruction, parsed.intensityOmitted)
  }

  override fun narrowTask(taskId: TaskId, narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    engine.narrowTask(taskId, parsed.instruction, parsed.intensityOmitted)
  }

  override fun canSelectTask(taskId: TaskId) = engine.canSelectTask(taskId)

  override fun canExecuteTask(taskId: TaskId) = engine.canExecuteTask(taskId)

  override fun selectTask(taskId: TaskId) = atomic { engine.selectTask(taskId) }

  override fun selectTask(instruction: String) = atomic {
    engine.selectTask(parse<Instruction>(instruction))
  }

  override fun doTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    engine.doTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
    )
  }

  override fun doTask(narrowing: String, taskId: TaskId) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    engine.doTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
        taskId,
    )
  }

  override fun tryTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    engine.tryTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
    )
  }

  override fun tryTask(narrowing: String, taskId: TaskId) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    engine.tryTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
        taskId,
    )
  }

  override fun tryTask(taskId: TaskId) = atomic { engine.tryTask(taskId) }

  // autoExecNow() and cross-Actor Agent calls can re-enter this call site. Its depth is shared
  // by every Actor in the world so only the true outermost operation drains and reports completion.
  private fun atomic(
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult =
      engine.transact(
          block = block,
          validateCompletion = validateCompletion,
          settle = { autoExecLoop.run(actor, autoExecPolicy) },
      )

  // Direct mutations preserve their legacy behavior of not invoking autoexecution.
  private fun atomicWithoutAutoExec(block: () -> Unit): TaskResult =
      engine.transact(settle = {}, block = block)

  private data class ParsedTaskNarrowing(
      val instruction: InstructionTree,
      val intensityOmitted: Boolean,
      val submittedAsGroup: Boolean,
  )
}
