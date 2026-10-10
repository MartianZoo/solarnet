package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class GmoContractTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for each matching tag on a played card`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$GmoContract, $Fish, $Decomposers, $AdaptedLichen")

    kim.playProject(AdvancedEcosystems, 11).expect("-5 MC")
  }
}
