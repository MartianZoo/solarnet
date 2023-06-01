package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Engine.PlayerScope
import dev.martianzoo.tfm.engine.Layers.NewOperationBody
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Instruction
import javax.inject.Inject

/**
 * An experiment in having a "generatable" class do the work of both parsing strings to PetElements,
 * adding atomicity, and producing TaskResults.
 */
@PlayerScope
internal class ApiTranslation
@Inject
constructor(
    private val reader: GameReader,
    private val timeline: Timeline,
    private val impl: Implementations,
) : Layers.Changes {

  override fun changesLayer() = this as Layers.Changes
  override fun tasksLayer() = this as Layers.Tasks
  override fun operationsLayer() = this as Layers.Operations
  override fun turnsLayer() = this as Layers.Turns

  // CHANGES

  override fun sneak(changes: String, fakeCause: Cause?) =
      timeline.atomic { impl.addTasks(parseInstruction(changes), fakeCause) }

  // TASKS

  override fun addTasks(instruction: String, firstCause: Cause?) =
      timeline.atomic { impl.addTasks(parseInstruction(instruction), firstCause) }

  override fun dropTask(taskId: TaskId) = impl.dropTask(taskId)

  // OPERATIONS

  override fun initiate(
      initialInstruction: String,
      autoExec: AutoExecMode,
      body: NewOperationBody.() -> Unit,
  ): TaskResult {
    return timeline.atomic {
      impl.operation(parseInstruction(initialInstruction)) {
        Adapter(this, autoExec).body()
      }
    }
  }

  private class Adapter(val apit: ApiTranslation, val autoExec: AutoExecMode) : NewOperationBody {
    override fun doTask(revised: String) {
      apit.doTask(revised)
      autoExecNow()
    }
    override fun tryTask(revised: String) {
      apit.tryTask(revised)
      autoExecNow()
    }
    override fun autoExecNow() {
      apit.impl.autoExec(autoExec)
    }
  }

  // GAMES (methods that can't break game-integrity)
  // This layer is only usable if you have a running workflow, so that >0 players always have a
  // task in their queue at any given time

  override fun reviseTask(taskId: TaskId, revised: String) =
      timeline.atomic { impl.reviseTask(taskId, parseInstruction(revised)) }

  override fun canPrepareTask(taskId: TaskId) = impl.canPrepareTask(taskId)

  override fun prepareTask(taskId: TaskId) = impl.prepareTask(taskId)

  override fun doTask(taskId: TaskId) = timeline.atomic { impl.doTask(taskId) }

  override fun doTask(revised: String) = timeline.atomic { impl.doTask(parseInstruction(revised)) }

  override fun tryTask(taskId: TaskId) = timeline.atomic { impl.tryTask(taskId) }

  override fun tryTask(revised: String) =
      timeline.atomic { impl.tryTask(parseInstruction(revised)) }

  override fun tryPreparedTask() = timeline.atomic { impl.tryPreparedTask() }

  // PRIVATE

  private fun parseInstruction(string: String): Instruction = reader.preprocess(parse(string))
}
