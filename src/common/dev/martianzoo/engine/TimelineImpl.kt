package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
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

  override fun commit() {
    commitFloor = checkpoint()
  }

  override fun rollBack(checkpoint: Checkpoint) {
    val ordinal = checkpoint.ordinal
    require(ordinal >= commitFloor.ordinal) {
      "Cannot roll back to $ordinal; committed through ${commitFloor.ordinal}"
    }
    require(ordinal <= gameWorld.nextOrdinal)
    rollBackStateTo(ordinal)
  }

  private fun rollBackStateTo(ordinal: Int) {
    changer.rollBackTo(ordinal)
    recordingPositions.rollBackTo(ordinal)
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
