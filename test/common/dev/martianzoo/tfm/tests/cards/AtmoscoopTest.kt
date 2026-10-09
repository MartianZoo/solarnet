package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AtmoscoopTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "you can choose to raise Temperature or Venus even if that parameter is maxed"
  @Test
  internal fun `Can choose an already-maxed Venus track`() {
    kim.exMachina("$AstraMechanica, $AtalantaPlanitiaLab, $IoSulphurResearch")
    kim.setToExMachina(15, "VenusStep")

    kim.playProject(Atmoscoop, 22) { doTask("2 VenusStep") }
        .expect("0 TemperatureStep, 0 VenusStep")
  }
}
