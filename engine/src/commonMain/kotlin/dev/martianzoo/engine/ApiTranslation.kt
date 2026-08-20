package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.tfm.data.Prod
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset
import kotlin.reflect.KClass

/**
 * An experiment in having a "generatable" class do the work of both parsing strings to PetElements,
 * adding atomicity, and producing TaskResults.
 */
internal class ApiTranslation(
    override val actor: Actor,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val impl: Implementations,
    private val tasks: TaskQueue,
    classTable: ClassTable,
    xers: Transformers,
    vocabulary: Vocabulary,
    private val atomicOperationBoundary: AtomicOperationBoundary,
) : GodMode { // so it really implements all gameplay layers

  override var autoExecMode: AutoExecMode = FIRST
    set(newMode) {
      if (newMode != field) {
        field = newMode
        autoExecNow()
      }
    }

  override fun godMode(): GodMode = this

  // READ-ONLY

  override fun has(requirement: String) = reader.has(parse(requirement))

  override fun count(metric: String) = reader.count(parse<Metric>(metric))

  override fun list(type: String): Multiset<Expression> {
    val typeToList: Type = reader.resolve(parse(type))
    val allComponents: Multiset<Type> = reader.getComponents(typeToList)

    val result = HashMultiset<Expression>()
    typeToList.rootClass.directSubclasses().forEach { sub ->
      val matches = allComponents.filter { it.isSubtypeOf(sub.baseType) }
      if (matches.any()) {
        @Suppress("UNCHECKED_CAST") val types = matches.elements as Set<Type>
        result.add(lub(types)!!.expression, matches.size)
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
          xers.atomizer(),
          xers.insertDefaults(),
          (actor as? Player)?.let(::replaceOwnerWith),
          Prod.deprodify(classTable),
      )

  override fun parseInternal(type: KClass<out PetElement>, text: String): PetElement =
      preprocessor.transformElement(Parsing.parse(type, text))

  private fun parseInstructionTree(text: String): InstructionTree = parse(text)

  private fun parseInstructionGroup(text: String): InstructionGroup =
      InstructionGroup.of(parseInstructionTree(text))

  // CHANGES

  override fun sneak(changes: String, fakeCause: Cause?) = timeline.atomic {
    impl.sneak(parseInstructionGroup(changes), fakeCause)
  }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> =
      impl.addTasks(parseInstructionGroup(instruction), firstCause)

  override fun dropTask(taskId: TaskId) = impl.dropTask(taskId)

  override fun dropTasks() = impl.dropTasks()

  // OPERATIONS

  override fun manual(initialInstructions: String, body: BodyLambda): TaskResult {
    return atomic {
      impl.manual(parseInstructionGroup(initialInstructions), autoExecMode) { Adapter().body() }
    }
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
    return atomic { impl.complete(autoExecMode) { Adapter().body() } }
  }

  private inner class Adapter : OperationBody {
    override val tasks by this@ApiTranslation::tasks
    override val reader by this@ApiTranslation::reader

    override fun doTask(revised: String, taskNumber: Int?) {
      this@ApiTranslation.doTask(revised, taskNumber)
    }

    override fun tryTask(revised: String, taskNumber: Int?) {
      this@ApiTranslation.tryTask(revised, taskNumber)
    }

    override fun autoExecNow() {
      atomic {}
    }
  }

  override fun autoExecNow() = atomic {}

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

  override fun reviseTask(taskId: TaskId, revised: String) = timeline.atomic {
    impl.reviseTask(taskId, parseInstructionTree(revised))
  }

  override fun reviseTask(current: String, revised: String) = timeline.atomic {
    impl.reviseTask(parse<Instruction>(current), parseInstructionTree(revised))
  }

  override fun canPrepareTask(taskId: TaskId) = impl.canPrepareTask(taskId)

  override fun prepareTask(taskId: TaskId) = impl.prepareTask(taskId)

  override fun prepareTask(instruction: String) = impl.prepareTask(parse<Instruction>(instruction))

  override fun doTask(revised: String, taskNumber: Int?) = atomic {
    impl.doTask(parseInstructionTree(revised), taskNumber)
  }

  override fun tryTask(revised: String, taskNumber: Int?) = atomic {
    impl.tryTask(parseInstructionTree(revised), taskNumber)
  }

  override fun tryPreparedTask() = atomic { impl.tryPreparedTask() }

  // autoExecNow() and cross-Actor gameplay calls can re-enter this boundary. Its depth is shared
  // by every Actor in the world so only the true outermost operation reports completion.
  fun atomic(block: () -> Unit): TaskResult = atomicOperationBoundary.run {
    block()
    impl.autoExecNow(autoExecMode)
  }
}
