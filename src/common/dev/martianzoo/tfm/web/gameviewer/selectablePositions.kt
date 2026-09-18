package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.state.Checkpoint

/** Recording positions whose preceding step is visible, plus the initial and final positions. */
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
  if (selected.last() != positions.lastIndex) selected += positions.lastIndex
  return selected
}
