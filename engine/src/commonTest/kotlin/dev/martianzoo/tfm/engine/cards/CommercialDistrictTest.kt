package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class CommercialDistrictTest : CardTest() {
  @Test
  fun `between two cities, places Commercial District`() {
    newGame()
    val p2 = requireP2()

    p1.manual("PROD[Energy], CityTile<Tharsis_3_2>")
    p1.manual("CommercialDistrict") { doTask("CdTile<Tharsis_3_3>") }
    p2.manual("CityTile<Tharsis_3_4>")

    engine.phase("End")
    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }
}
