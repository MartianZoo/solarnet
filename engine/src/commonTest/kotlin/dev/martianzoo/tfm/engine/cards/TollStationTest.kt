package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class TollStationTest : CardTest() {
  @Test
  fun `with space tags split between players, adds Toll Station`() {
    newGame("TerraformingMars,TharsisMap,CorporateEraExpansion")
    val p2 = requireP2()
    // Tags must be added with the cards they depend on.
    p1.manual("AsteroidMining, TransNeptuneProbe").expect("2 SpaceTag")
    p2.manual("VestaShipyard, SpaceElevator, SolarWindPower").expect("3 SpaceTag")
    p1.manual("TollStation").expect("PROD[3 Megacredit]")
  }
}
