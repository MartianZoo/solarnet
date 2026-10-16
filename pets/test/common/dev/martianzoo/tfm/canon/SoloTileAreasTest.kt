package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SoloTileAreasTest {
  @Test
  internal fun standardPlacementFindsTwoLegalPairsOnEveryMap() {
    // Include zero-cost indexing, long greenery counts that wrap, and expensive city cards.
    val draws = listOf(listOf(0, 0, 36, 36), listOf(20, 18, 2, 9), listOf(31, 36, 0, 0))
    for (map in Canon.marsMapDefinitions) {
      for (costs in draws) {
        val areas = soloTileAreas(map, costs)
        val context = "${map.className}, costs $costs"
        assertEquals(4, areas.toSet().size, context)
        assertTrue(areas.all { it in map.areas }, context)
        assertTrue(areas.none { it.kind in setOf(cn("WaterArea"), cn("NoctisArea")) }, context)
        val (city1, greenery1, city2, greenery2) = areas
        val neighbors1 = map.areas.hexNeighbors(city1.row, city1.column)
        val neighbors2 = map.areas.hexNeighbors(city2.row, city2.column)
        assertTrue(city2 !in neighbors1, context)
        assertTrue(greenery1 in neighbors1, context)
        assertTrue(greenery2 in neighbors2, context)
      }
    }
  }
}
