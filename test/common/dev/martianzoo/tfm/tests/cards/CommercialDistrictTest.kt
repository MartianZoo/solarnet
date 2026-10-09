package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CommercialDistrictTest : CardTest() {
  @Test
  internal fun `Scores adjacent cities after placement`() {
    newGame()
    val p2 = requireP2()

    p1.runOperation("PROD[Energy], CityTile<Tharsis_3_2>")
    p1.runOperation("$CommercialDistrict") { placeTile(3, 3) }
    p2.runOperation("CityTile<Tharsis_3_4>")

    admin.runOperation("End FROM Phase")
    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }
}
