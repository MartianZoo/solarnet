package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RecyclonTest : TfmSandboxTest() {
  @Test
  internal fun `Its own building tag adds a starting microbe`() {
    newTestGame(kimCorporation = Recyclon, startAtCorporation = true)

    kim.playCorp(Recyclon) { doTask("Microbe<$Recyclon>") }.expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Gains a microbe when its owner plays a building card`() {
    newTestGame(kimCorporation = Recyclon, startAtCorporation = true)
    kim.playCorp(Recyclon) { doTask("Microbe<$Recyclon>") }
    startActionPhase()

    kim.playProject(Mine, 4).expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Converts accumulated microbes including the new building microbe into production`() {
    newTestGame(kimCorporation = Recyclon, startAtCorporation = true)
    kim.playCorp(Recyclon) { doTask("Microbe<$Recyclon>") }
    startActionPhase()
    kim.exMachina("2 Microbe<$Recyclon>")

    kim.playProject(TitaniumMine, 7) { doTask("-2 Microbe THEN PROD[Plant]") }
        .expect("-2 Microbe, PROD[Plant]")
  }
}
