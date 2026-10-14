package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameRecording

/**
 * Captures the current completed event prefix and its positions without coupling playback to this
 * live World. A seeked World exports only the prefix through its current position; an incomplete
 * position cannot be exported.
 */
public fun World.recording(): GameRecording {
  val wholeWorld = this as? WholeWorld ?: error("Unknown World implementation: ${this::class}")
  check(timeline.checkpoint() in timeline.positions) {
    "Cannot record a World between completed gameplay positions"
  }
  val entries = events.entriesSince(Checkpoint(0))
  val positions = timeline.positions.filter { it.ordinal <= entries.size }
  return GameRecording(wholeWorld.gameWorld.premise, entries, positions)
}
