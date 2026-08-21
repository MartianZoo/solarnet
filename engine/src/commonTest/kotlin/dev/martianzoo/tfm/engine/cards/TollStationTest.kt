package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class TollStationTest : CardTest() {
  @Test
  fun `Counts opponents' space tags for its production gain`() {
    newGame()
    val p2 = requireP2()
    // Tags must be added with the cards they depend on.
    p2.manual("$VestaShipyard, $SpaceElevator, $SolarWindPower")

    p1.manual("$TollStation").expect("PROD[3 Megacredit]")
  }

  @Test
  fun `Adds no production without an opponent's space tags`() {
    newGame()

    p1.manual("$TollStation").expect("PROD[0 Megacredit]")
  }
}
