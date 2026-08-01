package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class TollStationTest : CardTest() {
  @Test
  fun `with opponent space tags, adds production for those tags`() {
    newGame("")
    val p2 = requireP2()
    // Tags must be added with the cards they depend on.
    p2.manual("VestaShipyard, SpaceElevator, SolarWindPower")

    p1.manual("TollStation").expect("PROD[3 Megacredit]")
  }

  @Test
  fun `with no opponent space tags, adds no production`() {
    newGame("")

    p1.manual("TollStation").expect("PROD[0 Megacredit]")
  }
}
