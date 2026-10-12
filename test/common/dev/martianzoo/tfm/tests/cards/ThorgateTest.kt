package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ThorGateTest : TfmSandboxTest() {
  @Test
  internal fun `Discounts power-production standard projects before payment`() {
    newTestGame(kimCorporation = ThorGate, startAtCorporation = true)
    kim.inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<ThorGate>>")
      doTask("ThorGate FROM CorporationCard")
      doTask("48 MC")
      doTask("PROD[Energy]")
      doTask("-30 MC")
      doTask("10 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()
    kim.setToExMachina(8, "MC")

    kim.inTurn { doTasks("UseStandardProject<PowerPlantProject>", "-8 MC", "PROD[Energy]") }
  }
}
