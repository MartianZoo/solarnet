package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Engine.PlayerScope
import dev.martianzoo.tfm.engine.Layers.Changes
import dev.martianzoo.tfm.engine.Layers.Tasks
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
) : Changes, Tasks {

  override fun addTasks(instruction: String, firstCause: Cause?) =
      timeline.atomic { impl.addTasks(parseInstruction(instruction), firstCause) }

  override fun dropTask(taskId: TaskId) = timeline.atomic { impl.dropTask(taskId) }

  override fun sneak(changes: String, fakeCause: Cause?) =
      timeline.atomic { impl.addTasks(parseInstruction(changes), fakeCause) }

  override fun reviseTask(taskId: TaskId, revised: String) =
      timeline.atomic { impl.reviseTask(taskId, parseInstruction(revised)) }

  override fun canPrepareTask(taskId: TaskId) = impl.canPrepareTask(taskId)

  override fun prepareTask(taskId: TaskId) = impl.prepareTask(taskId)

  override fun executeTask(taskId: TaskId) = timeline.atomic { impl.executeTask(taskId) }

  override fun explainTask(taskId: TaskId, reason: String) = impl.explainTask(taskId, reason)

  private fun parseInstruction(string: String): Instruction = reader.preprocess(parse(string))
}
