package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ThorGateTest : TfmSandboxTest() {
  @Test
  internal fun `Discounts power-production standard projects before payment`() {
    newTestGame(kimCorporation = ThorGate)
    kim.setToExMachina(8, "MC")

    kim.stdProject("PowerPlantProject").expect("-8 MC, PROD[Energy]")
  }
}
