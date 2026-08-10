package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon.Option.UtopiaPlanitiaMapOption
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test
import kotlin.test.assertFalse

class CapitalTest : CardTest() {
  @Test
  fun `places a regular city and scores its adjacent oceans`() {
    newGame()
    p1.manual("PROD[2 Energy], OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>")

    p1.manual("Capital") {
      doTask("CityTile<Tharsis_3_3>")
    }

    p1.assertCounts(
        1 to "CityTile<Tharsis_3_3>",
        1 to "CapitalMarker<CityTile<Tharsis_3_3>>",
        0 to "SpecialTile",
    )
    p1.manual("GreeneryTile<Tharsis_2_3>")
    p1.assertCounts(0 to "Adjacency<OwnedTile, SpecialTile>")
    engine.phase("End")
    p1.assertCounts(27 to "VictoryPoint")
  }

  @Test
  fun `does not count as a special tile for Specialist`() {
    newGame(UtopiaPlanitiaMapOption)
    p1.manual("PROD[2 Energy], EzTile<UtopiaPlanitia_2_2>, NpTile<UtopiaPlanitia_3_3>")
    p1.manual("Capital") {
      doTask("CityTile<UtopiaPlanitia_1_1>")
    }

    assertFalse(p1.has("3 SpecialTile"))
  }

  @Test
  fun `loses its marker and ocean scoring if its city is removed`() {
    newGame()
    p1.manual("PROD[2 Energy], OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>")
    p1.manual("Capital") { doTask("CityTile<Tharsis_3_3>") }

    p1.manual("-CityTile<Tharsis_3_3>")

    p1.assertCounts(0 to "CityTile<Tharsis_3_3>", 0 to "CapitalMarker")
    engine.phase("End")
    p1.assertCounts(22 to "VictoryPoint")
  }
}
