package dev.martianzoo.viewer

import dev.martianzoo.engine.Timeline.Checkpoint

/** Recording positions whose preceding displayed step contains a visible log event. */
internal fun selectablePositionIndices(
    positions: List<Checkpoint>,
    visibleEventOrdinals: List<Int>,
): List<Int> {
  require(positions.isNotEmpty())
  require(positions == positions.sortedBy(Checkpoint::ordinal))

  val selected = mutableListOf(0)
  for (candidateIndex in 1..positions.lastIndex) {
    val previousOrdinal = positions[selected.last()].ordinal
    val candidateOrdinal = positions[candidateIndex].ordinal
    if (visibleEventOrdinals.any { it >= previousOrdinal && it < candidateOrdinal }) {
      selected += candidateIndex
    }
  }
  return selected
}
