package dev.martianzoo.engine

import dev.martianzoo.state.GameRecording

/**
 * Captures this World's visible event prefix as an independent passive recording. Event commentary
 * is copied; later gameplay, navigation, or commentary edits do not alter the result. A World
 * positioned in its recorded past exports only the prefix through that position, leaving its
 * retained future untouched.
 *
 * The current transaction must be complete. Pending tasks may remain there: exporting does not
 * finish a game action or execute Admin work. Playback uses recorded events rather than engine
 * execution. The recording retains the premise, not runtime custom-class implementations or client
 * objects.
 *
 * @throws IllegalStateException if this World was not created by [Engine], its current position is
 *   inside a transaction, or a backing source has invalidated it
 */
public fun World.recording(): GameRecording {
  val wholeWorld = this as? WholeWorld ?: error("Unknown World implementation: ${this::class}")
  timeline.checkpoint() // Refuse a partial transaction.
  val entries = events.entriesSince(0)
  return GameRecording(wholeWorld.gameWorld.premise, entries)
}
