package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameRecording

/**
 * Captures this World's visible event prefix and its recorded positions as an independent passive
 * recording. Event commentary is copied; later gameplay, navigation, or commentary edits do not
 * alter the result. A World positioned in its recorded past exports only the prefix through that
 * position, leaving its retained future untouched.
 *
 * The current position must be recorded. Pending tasks may remain there: exporting does not finish
 * a game action or execute Admin work. Playback uses recorded events rather than engine execution.
 * The recording retains the premise, not runtime custom-class implementations or client objects.
 *
 * @throws IllegalStateException if this World was not created by [Engine], its current position is
 *   not recorded, or a backing source has invalidated it
 */
public fun World.recording(): GameRecording {
  val wholeWorld = this as? WholeWorld ?: error("Unknown World implementation: ${this::class}")
  check(timeline.checkpoint() in timeline.positions) {
    "Cannot export a World at an unrecorded position"
  }
  val entries = events.entriesSince(Checkpoint(0))
  val positions = timeline.positions.filter { it.ordinal <= entries.size }
  return GameRecording(wholeWorld.gameWorld.premise, entries, positions)
}
