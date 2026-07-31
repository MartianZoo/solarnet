package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class SupercapacitorsTest : CardTest() {
  @Test
  fun `with Supercapacitors, runs production`() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    p1.manual("PROD[3 Energy, 5 Heat], 3 Energy, 9 Heat, Supercapacitors")
    p1.assertCounts(3 to "Energy", 9 to "Heat")
    engine.phase("Production") { p1.doTask("2 Heat FROM Energy!") }
    p1.assertCounts(4 to "Energy", 16 to "Heat")
  }
}
