package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TollStationTest : CardTest() {
  @Test
  internal fun `Counts opponents' space tags for its production gain`() {
    newGame()
    val p2 = requireP2()
    // Tags must be added with the cards they depend on.
    p2.manual("$VestaShipyard, $SpaceElevator, $SolarWindPower")

    p1.manual("$TollStation").expect("PROD[3 MC]")
  }

  @Test
  internal fun `Adds no production without an opponent's space tags`() {
    newGame()

    p1.manual("$TollStation").expect("PROD[0 MC]")
  }
}
