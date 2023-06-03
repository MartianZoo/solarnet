package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameReader.Companion.parse
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.AutoExecMode.SAFE
import dev.martianzoo.tfm.engine.Engine.PlayerScope
import dev.martianzoo.tfm.engine.Layers.OperationBody
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import javax.inject.Inject

/**
 * An experiment in having a "generatable" class do the work of both parsing strings to PetElements,
 * adding atomicity, and producing TaskResults.
 */
@PlayerScope
internal class ApiTranslation
@Inject
constructor(
    private val writer: GameWriter,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val impl: Implementations,
    private val player: Player,
    private val tasks: TaskQueue,
) : Layers.Changes { // pulls in the other layers too

  override var autoExecMode: AutoExecMode = SAFE

  override fun changesLayer() = this as Layers.Changes
  override fun tasksLayer() = this as Layers.Tasks
  override fun operationsLayer() = this as Layers.Operations
  override fun turnsLayer() = this as Layers.Turns

  // READ-ONLY

  override fun has(requirement: String) = reader.has(parse(requirement))

  override fun count(metric: String) = reader.count(parse<Metric>(metric))

  private inline fun <reified P : PetElement> parse(text: String): P =
      replaceOwnerWith(player).transform(reader.parse(text))

  // CHANGES

  override fun sneak(changes: String, fakeCause: Cause?) =
      writer.sneak(changes, fakeCause)

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?) =
      timeline.atomic { impl.addTasks(parse(instruction), firstCause) }

  override fun dropTask(taskId: TaskId) = impl.dropTask(taskId)

  // OPERATIONS

  override fun initiate(initialInstruction: String, body: BodyLambda): TaskResult {
    return timeline.atomic {
      impl.operation(parse(initialInstruction), autoExecMode) {
        Adapter().body()
      }
    }
  }

  override fun complete(body: BodyLambda): TaskResult {
    return timeline.atomic {
      impl.complete(autoExecMode) {
        Adapter().body()
      }
    }
  }

  private inner class Adapter : OperationBody {
    override val tasks by this@ApiTranslation::tasks
    override val reader by this@ApiTranslation::reader

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
      initiate("NewTurn<$player>", body)
    } else {
      complete(body)
    }
  }

  override fun turn2(body: BodyLambda) = initiate("NewTurn2<$player>", body)

  // GAMES (methods that can't break game-integrity)
  // This layer is only usable if you have a running workflow, so that >0 players always have a
  // task in their queue at any given time

  override fun reviseTask(taskId: TaskId, revised: String) =
      timeline.atomic { impl.reviseTask(taskId, parse(revised)) }

  override fun canPrepareTask(taskId: TaskId) = impl.canPrepareTask(taskId)

  override fun prepareTask(taskId: TaskId) = impl.prepareTask(taskId)

  override fun doTask(taskId: TaskId) = timeline.atomic { impl.doTask(taskId) }

  override fun doTask(revised: String) = timeline.atomic { impl.doTask(parse(revised)) }

  override fun tryTask(taskId: TaskId) = timeline.atomic { impl.tryTask(taskId) }

  override fun tryTask(revised: String) = timeline.atomic { impl.tryTask(parse(revised)) }

  override fun tryPreparedTask() = timeline.atomic { impl.tryPreparedTask() }
}
