package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.engine.Implementations
import dev.martianzoo.engine.TaskQueue
import dev.martianzoo.engine.WorldTransaction
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
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
    override val actor: Actor,
    override val reader: GameReader,
    private val impl: Implementations,
    override val tasks: TaskQueue,
    private val elaborator: PetElaborator,
    private val worldTransaction: WorldTransaction,
) : Agent {

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
    impl.sneak(parseInstructionGroup(changes), fakeCause)
  }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> {
    var added = emptyList<TaskId>()
    atomicWithoutAutoExec { added = impl.addTasks(parseInstructionGroup(instruction), firstCause) }
    return added
  }

  override fun dropTask(taskId: TaskId): TaskRemovedEvent {
    lateinit var removed: TaskRemovedEvent
    atomicWithoutAutoExec { removed = impl.dropTask(taskId) }
    return removed
  }

  // OPERATIONS

  override fun runOperation(initialInstructions: String, body: OperationBlock): TaskResult {
    var allowedPendingTasks = emptySet<TaskId>()
    return atomic(
        block = {
          allowedPendingTasks =
              impl.runOperation(parseInstructionGroup(initialInstructions), autoExecPolicy) {
                Adapter().body()
              }
        },
        validateCompletion = { impl.requireComplete(allowedPendingTasks) },
    )
  }

  override fun beginOperation(initialInstructions: String, body: OperationBlock): TaskResult {
    return atomic {
      impl.beginOperation(parseInstructionGroup(initialInstructions), autoExecPolicy) {
        Adapter().body()
      }
    }
  }

  override fun continueOperation(body: OperationBlock): TaskResult {
    return atomic { impl.continueOperation(autoExecPolicy) { Adapter().body() } }
  }

  override fun completeOperation(body: OperationBlock): TaskResult {
    return atomic(
        block = { impl.complete(autoExecPolicy) { Adapter().body() } },
        validateCompletion = { impl.requireComplete() },
    )
  }

  private inner class Adapter : OperationScope {
    override val tasks = this@AgentImpl.tasks

    override val reader = this@AgentImpl.reader

    override fun doTask(narrowing: String) {
      this@AgentImpl.doTask(narrowing)
      impl.autoExecNow(autoExecPolicy)
    }

    override fun doTask(narrowing: String, taskId: TaskId) {
      this@AgentImpl.doTask(narrowing, taskId)
      impl.autoExecNow(autoExecPolicy)
    }

    override fun tryTask(narrowing: String) {
      this@AgentImpl.tryTask(narrowing)
      impl.autoExecNow(autoExecPolicy)
    }

    override fun tryTask(narrowing: String, taskId: TaskId) {
      this@AgentImpl.tryTask(narrowing, taskId)
      impl.autoExecNow(autoExecPolicy)
    }

    override fun autoExecNow() {
      impl.autoExecNow(autoExecPolicy)
    }
  }

  override fun autoExecNow() = atomic {}

  private fun autoExecAtomically(): TaskResult =
      worldTransaction.run({ impl.autoExecNow(autoExecPolicy) }) {}

  // TURNS

  override fun startTurn() = atomic { impl.startTurn() }

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
    impl.narrowTask(parsed.instruction, parsed.intensityOmitted)
  }

  override fun narrowTask(taskId: TaskId, narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.narrowTask(taskId, parsed.instruction, parsed.intensityOmitted)
  }

  override fun canSelectTask(taskId: TaskId) = impl.canSelectTask(taskId)

  override fun canExecuteTask(taskId: TaskId) = impl.canExecuteTask(taskId)

  override fun selectTask(taskId: TaskId) = atomic { impl.selectTask(taskId) }

  override fun selectTask(instruction: String) = atomic {
    impl.selectTask(parse<Instruction>(instruction))
  }

  override fun doTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.doTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
    )
  }

  override fun doTask(narrowing: String, taskId: TaskId) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.doTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
        taskId,
    )
  }

  override fun tryTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.tryTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
    )
  }

  override fun tryTask(narrowing: String, taskId: TaskId) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.tryTask(
        parsed.instruction,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
        taskId,
    )
  }

  override fun tryTask(taskId: TaskId) = atomic { impl.tryTask(taskId) }

  // autoExecNow() and cross-Actor Agent calls can re-enter this call site. Its depth is shared
  // by every Actor in the world so only the true outermost operation drains and reports completion.
  private fun atomic(
      validateCompletion: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult =
      worldTransaction.run(
          block = block,
          validateCompletion = validateCompletion,
          settle = { impl.autoExecNow(autoExecPolicy) },
      )

  // Direct mutation did not invoke legacy autoexecution before it joined the shared command scope.
  // Policy-driven advancement will replace this distinction in the later scheduler slice.
  private fun atomicWithoutAutoExec(block: () -> Unit): TaskResult = worldTransaction.run(block) {}

  private data class ParsedTaskNarrowing(
      val instruction: InstructionTree,
      val intensityOmitted: Boolean,
      val submittedAsGroup: Boolean,
  )
}
