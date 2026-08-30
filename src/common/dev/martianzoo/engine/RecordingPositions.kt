package dev.martianzoo.engine

/** Event-log positions after completed outer gameplay operations and automatic follow-up work. */
internal class RecordingPositions {
  private val ordinals = mutableListOf<Int>()

  internal fun record(ordinal: Int) {
    if (ordinals.lastOrNull() != ordinal) ordinals += ordinal
  }

  internal fun rollBackTo(ordinal: Int) {
    while (ordinals.lastOrNull()?.let { it > ordinal } == true) ordinals.removeLast()
  }

  internal fun snapshot(): List<Timeline.Checkpoint> = ordinals.map(Timeline::Checkpoint)
}
