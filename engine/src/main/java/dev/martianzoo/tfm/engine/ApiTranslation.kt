package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.AutoExecMode.FIRST
import dev.martianzoo.tfm.engine.Engine.PlayerScoped
import dev.martianzoo.tfm.engine.Gameplay.Companion.parse
import dev.martianzoo.tfm.engine.Gameplay.OperationBody
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * An experiment in having a "generatable" class do the work of both parsing strings to PetElements,
 * adding atomicity, and producing TaskResults.
 */
@PlayerScoped
internal class ApiTranslation
@Inject
constructor(
    private val writer: GameWriter,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val impl: Implementations,
    override val player: Player,
    private val tasks: TaskQueue,
) : Gameplay.GodMode { // so it really implements all gameplay layers

  override var autoExecMode: AutoExecMode = FIRST
    set(newMode) {
      if (newMode != field) {
        field = newMode
        autoExecNow()
      }
    }

  override fun godMode(): Gameplay.GodMode = this

  // READ-ONLY

  override fun has(requirement: String) = reader.has(parse(requirement))

  override fun count(metric: String) = reader.count(parse<Metric>(metric))

  override fun list(type: String): Multiset<Expression> {
    val typeToList: MType = reader.resolve(parse(type)) as MType
    val allComponents: Multiset<out Type> = reader.getComponents(typeToList)

    val result = HashMultiset<Expression>()
    typeToList.root.directSubclasses.forEach { sub ->
      val matches = allComponents.filter { it.narrows(sub.baseType) }
      if (matches.any()) {
        @Suppress("UNCHECKED_CAST")
        val types = matches.elements as Set<MType>
        result.add(lub(types)!!.expression, matches.size)
      }
    }
    return result
  }

  override fun resolve(expression: String) = reader.resolve(parse(expression))

  override fun <P : PetElement> parseInternal(type: KClass<P>, text: String): P =
      replaceOwnerWith(player).transform(reader.parseInternal(type, text))

  // CHANGES

  override fun sneak(changes: String, fakeCause: Cause?) =
      timeline.atomic { writer.sneak(parse(changes), fakeCause) }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?): List<TaskId> =
      impl.addTasks(parse(instruction), firstCause)

  override fun dropTask(taskId: TaskId) = impl.dropTask(taskId)

  // OPERATIONS

  override fun manual(initialInstruction: String, body: BodyLambda): TaskResult {
    return timeline.atomic {
      impl.manual(parse(initialInstruction), autoExecMode) { Adapter().body() }
    }
  }

  override fun beginManual(initialInstruction: String, body: BodyLambda): TaskResult {
    return timeline.atomic {
      impl.beginManual(parse(initialInstruction), autoExecMode) { Adapter().body() }
    }
  }

  override fun finish(body: BodyLambda): TaskResult {
    return timeline.atomic { impl.complete(autoExecMode) { Adapter().body() } }
  }

  private inner class Adapter : OperationBody {
    override val tasks by this@ApiTranslation::tasks
    override val reader by this@ApiTranslation::reader

    override fun doFirstTask(revised: String) {
      this@ApiTranslation.doFirstTask(revised)
      impl.autoExecNow(autoExecMode)
    }

    override fun doTask(revised: String) {
      this@ApiTranslation.doTask(revised)
      impl.autoExecNow(autoExecMode)
    }

    override fun tryTask(revised: String) {
      this@ApiTranslation.tryTask(revised)
      impl.autoExecNow(autoExecMode)
    }

    override fun autoExecNow() = impl.autoExecNow(autoExecMode)
  }

  override fun autoExecNow() = timeline.atomic { impl.autoExecNow(autoExecMode) }

  // TURNS

  override fun startTurn() = timeline.atomic { impl.startTurn() }
  override fun startTurn2() = timeline.atomic { impl.startTurn2() }

  // TODO unclear when we delegate straight to impl and when we do it ourselves
  override fun turn(body: BodyLambda): TaskResult {
    // TODO tighten this up somehow...
    return if (tasks.isEmpty()) {
      manual("NewTurn", body)
    } else {
      finish(body)
    }
  }

  override fun turn2(body: BodyLambda) = manual("NewTurn2<$player>", body)

  // GAMES (methods that can't break game-integrity)
  // This layer is only usable if you have a running workflow, so that >0 players always have a
  // task in their queue at any given time

  override fun reviseTask(taskId: TaskId, revised: String) =
      timeline.atomic { impl.reviseTask(taskId, parse(revised)) }

  override fun canPrepareTask(taskId: TaskId) = impl.canPrepareTask(taskId)

  override fun prepareTask(taskId: TaskId) = impl.prepareTask(taskId)

  override fun doFirstTask(revised: String?) =
      timeline.atomic { impl.doFirstTask(revised?.let { parse(it) }) }

  override fun doTask(taskId: TaskId) = timeline.atomic { impl.doTask(taskId) }

  override fun doTask(revised: String) = timeline.atomic { impl.doTask(parse(revised)) }

  override fun tryTask(taskId: TaskId) = timeline.atomic { impl.tryTask(taskId) }

  override fun tryTask(revised: String) = timeline.atomic { impl.tryTask(parse(revised)) }

  override fun tryPreparedTask() = timeline.atomic { impl.tryPreparedTask() }
}
