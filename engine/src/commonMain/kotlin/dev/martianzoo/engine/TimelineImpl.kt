package dev.martianzoo.engine

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.TaskEvent
import dev.martianzoo.pets.data.TaskResult

/**
 * Supports checkpoints and rollbacks to those checkpoints (and thereby, failure-atomic
 * interactions).
 */
internal class TimelineImpl(
    private val reader: GameReader,
    private val components: ComponentGraph,
    private val events: EventLog,
    private val tasks: TaskQueues,
) : Timeline {

  override fun checkpoint() = Checkpoint(events.size)

  private var commitFloor = Checkpoint(events.firstLocalOrdinal)

  override fun commit() {
    commitFloor = checkpoint()
  }

  override fun rollBack(checkpoint: Checkpoint) {
    val ordinal = checkpoint.ordinal
    require(ordinal >= commitFloor.ordinal) {
      "Cannot roll back to $ordinal; committed through ${commitFloor.ordinal}"
    }
    require(ordinal <= events.size)
    events.rollBackTo(ordinal) { entry ->
      when (entry) {
        is TaskEvent -> tasks.reverse(entry)
        is ChangeEvent ->
            with(entry.change) {
              components.applyChange(
                  count = count,
                  gaining = removing?.toComponent(reader),
                  removing = gaining?.toComponent(reader),
              )
            }
      }
    }
  }

  internal class AbortOperationException : Exception()

  @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
  override fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (_: AbortOperationException) {
      rollBack(checkpoint)
    } catch (e: Exception) {
      rollBack(checkpoint)
      throw e
    }
    return events.activitySince(checkpoint)
  }

  internal fun initializationFinished() = events.markSetupStart()
}
