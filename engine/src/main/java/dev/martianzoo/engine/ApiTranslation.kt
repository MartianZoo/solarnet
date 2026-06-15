package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
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
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.tfm.engine.Prod
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset
import kotlin.reflect.KClass

/**
 * An experiment in having a "generatable" class do the work of both parsing strings to PetElements,
 * adding atomicity, and producing TaskResults.
 */
internal class ApiTranslation(
    public override val player: Player,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val impl: Implementations,
    private val tasks: TaskQueue,
    table: MClassTable,
    xers: Transformers,
) : GodMode { // so it really implements all gameplay layers

  public override var autoExecMode: AutoExecMode = FIRST
    set(newMode) {
      if (newMode != field) {
        field = newMode
        autoExecNow()
      }
    }

  public override fun godMode(): GodMode = this

  // READ-ONLY

  public override fun has(requirement: String) = reader.has(parse(requirement))

  public override fun count(metric: String) = reader.count(parse<Metric>(metric))

  public override fun list(type: String): Multiset<Expression> {
    val typeToList: MType = reader.resolve(parse(type)) as MType
    val allComponents: Multiset<out Type> = reader.getComponents(typeToList)

    val result = HashMultiset<Expression>()
    typeToList.root.directSubclasses().forEach { sub ->
      val matches = allComponents.filter { it.narrows(sub.baseType) }
      if (matches.any()) {
        @Suppress("UNCHECKED_CAST") val types = matches.elements as Set<MType>
        result.add(lub(types)!!.expression, matches.size)
      }
    }
    return result
  }

  public override fun resolve(expression: String) = reader.resolve(parse(expression))

  private val preprocessor =
      chain(
          xers.useFullNames(),
          xers.atomizer(),
          xers.insertDefaults(),
          replaceOwnerWith(player),
          Prod.deprodify(table),
      )

  public override fun <P : PetElement> parseInternal(type: KClass<P>, text: String) =
      preprocessor.transform(Parsing.parse(type, text))

  // CHANGES

  public override fun sneak(changes: String, fakeCause: Cause?) =
      timeline.atomic { impl.sneak(parse(changes), fakeCause) }

  // TASKS

  public override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> =
      impl.addTasks(parse(instruction), firstCause)

  public override fun dropTask(taskId: TaskId) = impl.dropTask(taskId)

  // OPERATIONS

  public override fun manual(initialInstruction: String, body: BodyLambda): TaskResult {
    return atomic { impl.manual(parse(initialInstruction), autoExecMode) { Adapter().body() } }
  }

  public override fun beginManual(initialInstruction: String, body: BodyLambda): TaskResult {
    return atomic { impl.beginManual(parse(initialInstruction), autoExecMode) { Adapter().body() } }
  }

  public override fun continueManual(body: BodyLambda): TaskResult {
    return atomic { impl.continueManual(autoExecMode) { Adapter().body() } }
  }

  public override fun finish(body: BodyLambda): TaskResult {
    return atomic { impl.complete(autoExecMode) { Adapter().body() } }
  }

  private inner class Adapter : OperationBody {
    public override val tasks by this@ApiTranslation::tasks
    public override val reader by this@ApiTranslation::reader

    public override fun doFirstTask(revised: String) {
      this@ApiTranslation.doFirstTask(revised)
    }

    public override fun doTask(revised: String) {
      this@ApiTranslation.doTask(revised)
    }

    public override fun tryTask(revised: String) {
      this@ApiTranslation.tryTask(revised)
    }

    public override fun autoExecNow() {
      atomic {}
    }
  }

  public override fun autoExecNow() = atomic {}

  // TURNS

  public override fun startTurn() = atomic { impl.startTurn() }

  public override fun turn(body: BodyLambda): TaskResult {
    return if (tasks.isEmpty()) {
      manual("NewTurn", body)
    } else {
      finish(body)
    }
  }

  // GAMES (methods that can't break game-integrity)
  // This layer is only usable if you have a running workflow, so that >0 players always have a
  // task in their queue at any given time

  public override fun reviseTask(taskId: TaskId, revised: String) =
      timeline.atomic { impl.reviseTask(taskId, parse(revised)) }

  public override fun canPrepareTask(taskId: TaskId) = impl.canPrepareTask(taskId)

  public override fun prepareTask(taskId: TaskId) = impl.prepareTask(taskId)

  public override fun doFirstTask(revised: String?) = atomic {
    impl.doFirstTask(revised?.let { parse(it) })
  }

  public override fun doTask(taskId: TaskId) = atomic { impl.doTask(taskId) }

  public override fun doTask(revised: String) = atomic { impl.doTask(parse(revised)) }

  public override fun tryTask(taskId: TaskId) = atomic { impl.tryTask(taskId) }

  public override fun tryTask(revised: String) = atomic { impl.tryTask(parse(revised)) }

  public override fun tryPreparedTask() = atomic { impl.tryPreparedTask() }

  private fun atomic(block: () -> Unit) =
      timeline.atomic {
        block()
        impl.autoExecNow(autoExecMode)
      }
}
