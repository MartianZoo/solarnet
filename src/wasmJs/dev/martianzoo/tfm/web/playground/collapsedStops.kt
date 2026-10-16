package dev.martianzoo.tfm.web.playground

/** Associates each visible row with the earliest decision before it, plus a possible final gap. */
internal fun collapsedStops(stops: List<Int>, visibleEvents: List<Int>): Map<Int?, Int> {
  val result = linkedMapOf<Int?, Int>()
  var row = 0
  for (stop in stops) {
    while (row < visibleEvents.size && visibleEvents[row] < stop) row++
    val next = visibleEvents.getOrNull(row)
    if (next !in result) result[next] = stop
  }
  return result
}
