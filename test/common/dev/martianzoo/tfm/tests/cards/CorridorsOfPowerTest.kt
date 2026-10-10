package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CorridorsOfPowerTest : TfmSandboxTest() {
  @Test
  internal fun `Draws for gaining leadership rather than for every delegate placed`() {
    newTestGame("PreludeExpansion, CorridorsOfPower, TurmoilExpansion")
    kim.playPrelude(CorridorsOfPower)
    startActionPhase()

    kim.stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }.expect("ProjectCard")
    kim.stdAction("LobbyAction", 2, payment = { kim.pay(5) }) {
          doTask("PartyDelegate<Scientists>")
        }
        .expect("0 ProjectCard")
  }
}
