package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AtmoscoopTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "you can choose to raise Temperature or Venus even if that parameter is maxed"
  @Test
  internal fun `Can choose an already-maxed Venus track`() {
    kim.exMachina("$AstraMechanica, $AtalantaPlanitiaLab, $IoSulphurResearch")
    kim.setToExMachina(15, "VenusStep")

    kim.inTurn {
          doTasks(*playCardTasks("Atmoscoop"), "22 Pay<> FROM MC")
          doTasks("Atmoscoop FROM ProjectCard")
          doTask("2* Floater")
          doTask("2 VenusStep")
          doTask("VenusStep")
          doTask("VenusStep")
        }
        .expect("0 TemperatureStep, 0 VenusStep")
  }
}
