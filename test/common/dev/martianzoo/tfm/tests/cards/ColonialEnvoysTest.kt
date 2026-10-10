package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ColonialEnvoysTest : TfmSandboxTest() {
  @Test
  internal fun `Each owned colony supplies a delegate even when colonies share a tile`() {
    newTestGame("ColonialEnvoys, TurmoilExpansion, Luna, Io")
    kim.exMachina("2 Colony<Luna>, 2 PartyDelegate<Unity>")
    stan.exMachina("Colony<Io>")

    kim.playProject(ColonialEnvoys, 4) {
          doTask("PartyDelegate<Scientists>")
          doTask("PartyDelegate<Greens>")
        }
        .expect("PartyDelegate<Scientists>, PartyDelegate<Greens>, 0 PartyDelegate<Stan>")
  }
}
