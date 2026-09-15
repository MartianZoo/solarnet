package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameRecording

/**
 * Captures immutable events and completed positions without coupling playback to this live World.
 */
public fun World.recording(): GameRecording {
  val wholeWorld = this as? WholeWorld ?: error("Unknown World implementation: ${this::class}")
  val entries = events.entriesSince(Checkpoint(0))
  val positions =
      (wholeWorld.recordingPositions.snapshot().filter { it.ordinal <= entries.size } +
              Checkpoint(entries.size))
          .distinct()
  return GameRecording(wholeWorld.gameWorld.premise, entries, positions)
}
