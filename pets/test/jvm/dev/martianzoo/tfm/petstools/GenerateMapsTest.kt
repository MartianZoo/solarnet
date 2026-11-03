package dev.martianzoo.tfm.petstools

import dev.martianzoo.tfm.canon.MarsMapReader
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateMapsTest {
  @Test
  fun `seeded maps retain required bonuses and valid special packages`() {
    val random = Random(19)
    var noctisMaps = 0
    var venusMaps = 0
    var mapsWithDoubledVolcanicRewards = 0
    repeat(500) {
      val map = generateMap(random)
      assertEquals(61, map.areas.size)
      assertEquals(12, map.areas.count { it.kind.toString() == "WaterArea" })
      assertTrue(map.areas.count { it.kind.toString() == "VolcanicArea" } in setOf(0, 4))
      if (map.areas.any { it.code in listOf("VSS", "VTT", "VCC") }) mapsWithDoubledVolcanicRewards++
      val noctis = map.areas.count { it.kind.toString() == "NoctisArea" }
      assertTrue(noctis in 0..1)
      noctisMaps += noctis
      val codes = map.areas.flatMap { it.expandedBonusCodes }
      for (required in "PCST") assertTrue(required in codes, "Missing $required")
      val specialAreas = map.areas.filter { it.expandedBonusCodes.any { code -> code in "XVOF" } }
      assertTrue(specialAreas.size <= 1)
      specialAreas.forEach { area ->
        assertTrue(area.code in listOf("LX", "LV", "LO6", "LF4", "NX", "NV", "NO6", "NF4"))
      }
      venusMaps += map.areas.count { 'V' in it.expandedBonusCodes }
      map.areas
          .filter { 'V' in it.expandedBonusCodes }
          .forEach {
            assertEquals("VenusPlacementBonus", it.bonusText)
          }
    }
    assertTrue(noctisMaps in 70..130, "Noctis count: $noctisMaps")
    assertTrue(venusMaps > 0, "No Venus specials generated")
    assertTrue(mapsWithDoubledVolcanicRewards > 0, "No doubled volcanic rewards generated")
    val first = generateMap(Random(42))
    val second = generateMap(Random(42))
    assertEquals(renderDiagram(first), renderDiagram(second))
  }

  @Test
  fun `Noctis does not replace a bargain space`() {
    repeat(50) { seed ->
      val map =
          generateMap(Random(seed), withVolcanoes = true, withSpecial = true, withNoctis = true)
      assertEquals(4, map.areas.count { it.kind.toString() == "VolcanicArea" })
      assertEquals(1, map.areas.count { it.kind.toString() == "NoctisArea" })
      val special = map.areas.single { it.expandedBonusCodes.any { code -> code in "XVOF" } }
      assertEquals("LandArea", special.kind.toString())
    }
  }

  @Test
  fun `eleven connected water areas fail screening despite ordinary shared edge count`() {
    val diagram =
        """
        //               VS    LT    LC   LHH    VS
        //
        //            W     L     LP    L     L     L
        //
        //         LS    WP    LP    LT    L    LTT    L
        //
        //      L     LP   WPP    LP   WPSS   LC    LP    L
        //
        //   LC   LPT   LPP   WPP   WPP    WP   LPP    LP   LSS
        //
        //      VS    LS   LPP    L     WP    L     L     LS
        //
        //        LPP    LS   LHH    LH    WT   WHH    L
        //
        //           LPP    L    LTT    L     W     LC
        //
        //               W     VS    L     LC    LC
        """
            .trimIndent()
    val map =
        MarsMapReader.readMaps(
                "${MarsMapReader.GENERATED_AREAS_COMMENT}\n$diagram\nCLASS Generated_1_1 : LandArea"
            )
            .single()
    assertTrue("unusual water grouping" in mapOutliers(map))
  }
}
