package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TollStationTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Counts opponents' space tags`() {
    // Tags must be added with the cards they depend on.
    stan.exMachina("$VestaShipyard, $SpaceElevator, $SolarWindPower")

    kim.playProject(TollStation, 12).expect("PROD[3 MC]")
  }
}
