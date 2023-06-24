package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.TaskEvent
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.engine.Engine.Updater
import dev.martianzoo.engine.Timeline.Checkpoint
import javax.inject.Inject

/**
 * Supports checkpoints and rollbacks to those checkpoints (and thereby, failure-atomic
 * interactions).
 */
@GameScoped
internal class TimelineImpl @Inject constructor(
    private val reader: GameReader,
    private val updater: Updater,
    private val events: WritableEventLog,
    private val tasks: WritableTaskQueue,
) : Timeline {

  override fun checkpoint() = Checkpoint(events.size)

  override fun rollBack(checkpoint: Checkpoint) {

    val ordinal = checkpoint.ordinal
    require(ordinal <= events.size)
    if (ordinal == events.size) return

    val subList = events.eventsToRollBack(ordinal)
    for (entry in subList.asReversed()) {
      when (entry) {
        is TaskEvent -> tasks.reverse(entry)
        is ChangeEvent ->
          with(entry.change) {
            updater.update(
                count = count,
                gaining = removing?.toComponent(reader),
                removing = gaining?.toComponent(reader),
            )
          }
      }
    }
    subList.clear()
  }

  internal class AbortOperationException : Exception()

  override fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (e: Exception) {
      rollBack(checkpoint)
      if (e !is AbortOperationException) throw e
    }
    return events.activitySince(checkpoint)
  }

  internal fun initializationFinished() = events.setStartPoint()
}
