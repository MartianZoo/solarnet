package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ManutechTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for its own starting steel production`() {
    newTestGame(kimCorporation = Manutech, startAtCorporation = true)

    kim.playCorp(Manutech).expect("PROD[Steel], Steel")
  }

  @Test
  internal fun `Production gains replace the plants spent on Nitrophilic Moss`() {
    newTestGame(kimCorporation = Manutech)
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")
    kim.setToExMachina(2, "Plant")
    kim.setToExMachina(8, "MC")

    kim.playProject(NitrophilicMoss, 8).expect("PROD[2 Plant], 0 Plant")
  }
}
