package dev.martianzoo.tfm.web.playground

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CollapsedStopsTest {
  @Test
  fun hidingRowsKeepsOnlyTheFirstDecisionInEachGap() {
    val decisions = listOf(10, 12, 20, 24, 30, 35)
    assertEquals(
        linkedMapOf(15 to 10, 25 to 20, null to 30),
        collapsedStops(decisions, listOf(5, 15, 25)),
    )
    assertEquals(
        listOf(10, 12, 20, 24, 30, 35),
        collapsedStops(decisions, listOf(10, 12, 20, 24, 30, 35)).values.toList(),
    )
    assertEquals(mapOf<Int?, Int>(null to 10), collapsedStops(decisions, emptyList()))
  }
}
