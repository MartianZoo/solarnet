package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class GmoContractTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for each matching tag on a played card`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$GmoContract, $Fish, $Decomposers, $AdaptedLichen")

    kim.inTurn {
      doTasks(
          *playCardTasks("AdvancedEcosystems"),
          "11 Pay<> FROM MC",
          "AdvancedEcosystems FROM ProjectCard",
          "2 MC",
      )
      doTask("Microbe<Decomposers>*")
      doTask("2 MC")
      doTask("Microbe<Decomposers>*")
      doTask("2 MC")
      doTask("Microbe<Decomposers>*")
    }
  }
}
