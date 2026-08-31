package dev.martianzoo.engine

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent.Kind
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.inferTypeVariables
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.Multiset
import kotlin.reflect.KClass

/**
 * An experiment in having a "generatable" class do the work of both parsing strings to PetElements,
 * adding atomicity, and producing TaskResults.
 */
internal class ApiTranslation(
    override val actor: Actor,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val events: EventLog,
    private val impl: Implementations,
    private val tasks: TaskQueue,
    private val classTable: ClassTable,
    xers: Transformers,
    vocabulary: Vocabulary,
    private val atomicOperationScope: AtomicOperationScope,
) : Agent {

  override var autoExecMode: AutoExecMode = FIRST
    set(newMode) {
      if (newMode != field) {
        field = newMode
        autoExecAtomically()
      }
    }

  // READ-ONLY

  override fun has(requirement: String) = reader.has(parse(requirement))

  override fun count(metric: String) = reader.count(parse<Metric>(metric))

  override fun list(type: String): Multiset<Expression> {
    val typeToList: Type = reader.resolve(parse(type))
    val allComponents: Multiset<Type> = reader.getComponents(typeToList)

    val result = HashMultiset<Expression>()
    classTable.directSubclasses(typeToList.rootClass).forEach { sub ->
      val matches = allComponents.filter { it.isSubtypeOf(sub.baseType) }
      if (matches.any()) {
        @Suppress("UNCHECKED_CAST") val types = matches.elements as Set<Type>
        result.add(types.reduceOrNull(Type::lub)!!.expression, matches.size)
      }
    }
    return result
  }

  override fun resolve(expression: String) = reader.resolve(parse(expression))

  private val preprocessor =
      chain(
          xers.rejectPropertyEvaluations(),
          xers.canonicalize(vocabulary),
          xers.useFullNames(),
          classTable.inferTypeVariables(),
          xers.atomizer(),
          xers.insertDefaults(),
          (actor as? Player)?.let(::replaceOwnerWith),
          xers.transformMarkedSyntax(),
      )

  override fun parseInternal(type: KClass<out PetElement>, text: String): PetElement =
      preprocessor.transformElement(Parsing.parse(type, text))

  private fun parseTaskNarrowing(text: String): ParsedTaskNarrowing {
    val parsed = Parsing.parse<InstructionTree>(text)
    return ParsedTaskNarrowing(
        preprocessor.transformInstructionTree(parsed),
        intensityOmitted = parsed is Change && parsed.intensity == null,
        submittedAsGroup = parsed is InstructionGroup,
    )
  }

  private fun parseInstructionGroup(text: String): InstructionGroup =
      InstructionGroup.of(parse<InstructionTree>(text))

  // CHANGES

  override fun sneak(changes: String, fakeCause: Cause?): TaskResult {
    val operationStartOrdinal = timeline.checkpoint().ordinal
    return timeline.atomic {
      impl.sneak(parseInstructionGroup(changes), fakeCause)
      recordPlayerInput(Kind.DIRECT_CHANGES, changes, operationStartOrdinal = operationStartOrdinal)
    }
  }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> =
      impl.addTasks(parseInstructionGroup(instruction), firstCause)

  override fun dropTask(taskId: TaskId) = impl.dropTask(taskId)

  override fun dropTasks() = impl.dropTasks()

  // OPERATIONS

  override fun manual(initialInstructions: String, body: BodyLambda): TaskResult {
    var allowedPendingTasks = emptySet<TaskId>()
    return atomic(
        block = {
          allowedPendingTasks =
              impl.manual(parseInstructionGroup(initialInstructions), autoExecMode) {
                Adapter().body()
              }
        },
        afterIdleCleanup = { impl.requireComplete(allowedPendingTasks) },
    )
  }

  override fun beginManual(initialInstructions: String, body: BodyLambda): TaskResult {
    return atomic {
      impl.beginManual(parseInstructionGroup(initialInstructions), autoExecMode) {
        Adapter().body()
      }
    }
  }

  override fun continueManual(body: BodyLambda): TaskResult {
    return atomic { impl.continueManual(autoExecMode) { Adapter().body() } }
  }

  override fun finish(body: BodyLambda): TaskResult {
    return atomic(
        block = { impl.complete(autoExecMode) { Adapter().body() } },
        afterIdleCleanup = { impl.requireComplete() },
    )
  }

  private inner class Adapter : OperationBody {
    override val tasks = this@ApiTranslation.tasks

    override val reader = this@ApiTranslation.reader

    override fun doTask(narrowing: String, taskNumber: Int?) {
      this@ApiTranslation.doTask(narrowing, taskNumber)
      impl.autoExecNow(autoExecMode)
    }

    override fun tryTask(narrowing: String, taskNumber: Int?) {
      this@ApiTranslation.tryTask(narrowing, taskNumber)
      impl.autoExecNow(autoExecMode)
    }

    override fun autoExecNow() {
      impl.autoExecNow(autoExecMode)
    }
  }

  override fun autoExecNow() = atomic {}

  private fun autoExecAtomically(): TaskResult =
      atomicOperationScope.run({ impl.autoExecNow(autoExecMode) }) {}

  // TURNS

  override fun startTurn() = atomic { impl.startTurn() }

  override fun inTurn(body: BodyLambda): TaskResult {
    return if (tasks.isEmpty()) {
      manual("NewTurn", body)
    } else {
      finish(body)
    }
  }

  // GAMES (methods that can't break game-integrity)
  // This layer is only usable if you have a running workflow, so that >0 players always have a
  // task in their queue at any given time

  override fun narrowTask(narrowing: String) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.narrowTask(parsed.instruction, parsed.intensityOmitted)
    recordPlayerInput(Kind.NARROW_TASK, narrowing)
  }

  override fun canSelectTask(taskId: TaskId) = impl.canSelectTask(taskId)

  override fun selectTask(taskId: TaskId) = atomic {
    val task = tasks.getTaskData(taskId)
    val taskNumber = tasks.ids().indexOf(taskId) + 1
    impl.selectTask(taskId)
    recordPlayerInput(Kind.SELECT_TASK, task.instruction.toString(), taskNumber)
  }

  override fun selectTask(instruction: String) = atomic {
    impl.selectTask(parse<Instruction>(instruction))
    recordPlayerInput(Kind.SELECT_TASK, instruction)
  }

  override fun doTask(narrowing: String, taskNumber: Int?) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    impl.doTask(
        parsed.instruction,
        taskNumber,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
    )
    recordPlayerInput(Kind.DO_TASK, narrowing, taskNumber)
  }

  override fun tryTask(narrowing: String, taskNumber: Int?) = atomic {
    val parsed = parseTaskNarrowing(narrowing)
    val eventCount = events.size
    impl.tryTask(
        parsed.instruction,
        taskNumber,
        parsed.intensityOmitted,
        parsed.submittedAsGroup,
    )
    if (events.size != eventCount) recordPlayerInput(Kind.DO_TASK, narrowing, taskNumber)
  }

  // autoExecNow() and cross-Actor Agent calls can re-enter this call site. Its depth is shared
  // by every Actor in the world so only the true outermost operation drains and reports completion.
  private fun atomic(
      afterIdleCleanup: () -> Unit = {},
      block: () -> Unit,
  ): TaskResult =
      atomicOperationScope.run(
          block = block,
          afterIdleCleanup = afterIdleCleanup,
          beforeOutermostCompletion = { impl.autoExecNow(autoExecMode) },
      )

  private fun recordPlayerInput(
      kind: Kind,
      source: String,
      taskNumber: Int? = null,
      operationStartOrdinal: Int = atomicOperationScope.currentOperationStartOrdinal,
  ) {
    if (actor !is Player) return
    events.record(
        GameplayInputEvent(
            events.nextOrdinal,
            operationStartOrdinal,
            actor,
            kind,
            source,
            taskNumber,
        )
    ) {}
  }

  private data class ParsedTaskNarrowing(
      val instruction: InstructionTree,
      val intensityOmitted: Boolean,
      val submittedAsGroup: Boolean,
  )
}
