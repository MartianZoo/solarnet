package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class InterplanetaryTradeTest : TfmSandboxTest() {
  @Test
  internal fun `Ignores the tags of a previously played event`() {
    newTestGame()
    kim.exMachina("$Ecoline, $Mine, $SearchForLife")
    kim.setToExMachina(43, "MC")
    kim.playProject(ImportedHydrogen, 16) {
      doTask("3 Plant")
      placeTile(1, 2)
    }

    kim.playProject(InterplanetaryTrade, 27).expect("PROD[4 MC]")
  }
}
