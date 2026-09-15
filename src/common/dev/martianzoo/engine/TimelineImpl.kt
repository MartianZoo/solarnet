package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.TaskResult

/**
 * Supports checkpoints and rollbacks to those checkpoints (and thereby, failure-atomic
 * interactions).
 */
internal class TimelineImpl(
    private val gameWorld: GameWorld,
    private val changer: Changer,
    private val recordingPositions: RecordingPositions,
) : Timeline {

  override fun checkpoint() = Checkpoint(gameWorld.nextOrdinal)

  private var commitFloor = Checkpoint(0)
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
    require(ordinal <= gameWorld.nextOrdinal)
    rollBackStateTo(ordinal)
  }

  private fun rollBackStateTo(ordinal: Int) {
    changer.rollBackTo(ordinal)
    if (recordedRollbackOrdinals == null) recordingPositions.rollBackTo(ordinal)
  }

  internal fun seek(recordedEntries: List<GameEvent>, checkpoint: Checkpoint) {
    require(checkpoint.ordinal in 0..recordedEntries.size)
    if (gameWorld.nextOrdinal > checkpoint.ordinal) {
      rollBackStateTo(checkpoint.ordinal)
    } else {
      recordedEntries.subList(gameWorld.nextOrdinal, checkpoint.ordinal).forEach { entry ->
        if (entry is ChangeEvent) changer.replay(entry) else gameWorld.apply(entry)
      }
    }
  }

  internal fun sealRecording(positions: List<Checkpoint>) {
    check(recordedRollbackOrdinals == null) { "this timeline is already recorded" }
    recordedRollbackOrdinals = positions.mapTo(linkedSetOf()) { it.ordinal }
  }

  @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
  override fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (_: AbortTransactionException) {
      rollBackStateTo(checkpoint.ordinal)
    } catch (e: Exception) {
      rollBackStateTo(checkpoint.ordinal)
      throw e
    }
    return gameWorld.activitySince(checkpoint)
  }

  internal fun initializationFinished() = gameWorld.markSetupStart()
}
