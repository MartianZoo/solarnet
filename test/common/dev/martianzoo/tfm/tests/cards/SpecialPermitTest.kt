package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SpecialPermitTest : TfmSandboxTest() {
  @Test
  internal fun `Takes all four plants from the chosen opponent`() {
    newTestGame("SpecialPermit, TurmoilExpansion")
    stan.exMachina("4 Plant")
    rob.exMachina("4 Plant")

    kim.playProject(SpecialPermit, 5) { doTask("4 Plant<Kim FROM Stan>") }
        .expect("4 Plant, -4 Plant<Stan>, 0 Plant<Rob>")
  }
}
