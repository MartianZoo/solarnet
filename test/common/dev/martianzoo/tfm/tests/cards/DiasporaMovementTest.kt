package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class DiasporaMovementTest : TfmSandboxTest() {
  @Test
  internal fun `Counts its own Jovian tag and ignores an opponents tag`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<Reds>")
    stan.exMachina("$ColonizerTrainingCamp")

    kim.playProject(DiasporaMovement, 7).expect("-5 MC")
  }
}
