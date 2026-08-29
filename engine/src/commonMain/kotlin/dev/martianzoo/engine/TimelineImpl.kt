package dev.martianzoo.engine

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.GameEvent
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
    private val recordingPositions: RecordingPositions,
) : Timeline {

  override fun checkpoint() = Checkpoint(events.size)

  private var commitFloor = Checkpoint(events.firstLocalOrdinal)
  private var recordedRollbackOrdinals: Set<Int>? = null

  override fun commit() {
    commitFloor = checkpoint()
  }

  override fun rollBack(checkpoint: Checkpoint) {
    val ordinal = checkpoint.ordinal
    val recordedOrdinals = recordedRollbackOrdinals
    if (recordedOrdinals == null) {
      require(ordinal >= commitFloor.ordinal) {
        "Cannot roll back to $ordinal; committed through ${commitFloor.ordinal}"
      }
    } else {
      require(ordinal in recordedOrdinals) {
        "Cannot roll back to $ordinal; it is not a recorded position"
      }
    }
    require(ordinal <= events.size)
    rollBackStateTo(ordinal)
  }

  private fun rollBackStateTo(ordinal: Int) {
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
    if (recordedRollbackOrdinals == null) recordingPositions.rollBackTo(ordinal)
  }

  internal fun seek(recordedEntries: List<GameEvent>, checkpoint: Checkpoint) {
    require(checkpoint.ordinal in 0..recordedEntries.size)
    if (events.size > checkpoint.ordinal) {
      rollBackStateTo(checkpoint.ordinal)
    } else {
      recordedEntries.subList(events.size, checkpoint.ordinal).forEach { entry ->
        when (entry) {
          is TaskEvent -> tasks.replay(entry)
          is ChangeEvent ->
              with(entry.change) {
                events.record(entry) {
                  components.applyChange(
                      count = count,
                      gaining = gaining?.toComponent(reader),
                      removing = removing?.toComponent(reader),
                  )
                }
              }
        }
      }
    }
  }

  internal fun sealRecording(positions: List<Checkpoint>) {
    check(recordedRollbackOrdinals == null) { "this timeline is already recorded" }
    recordedRollbackOrdinals = positions.mapTo(linkedSetOf()) { it.ordinal }
  }

  internal class AbortOperationException : Exception()

  @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
  override fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (_: AbortOperationException) {
      rollBackStateTo(checkpoint.ordinal)
    } catch (e: Exception) {
      rollBackStateTo(checkpoint.ordinal)
      throw e
    }
    return events.activitySince(checkpoint)
  }

  internal fun initializationFinished() = events.markSetupStart()
}
