package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RiseToPowerTest : TfmSandboxTest() {
  @Test
  internal fun `Delegates can lead three different parties and trigger Corridors of Power for each`() {
    newTestGame("PreludeExpansion, RiseToPower, CorridorsOfPower, TurmoilExpansion")
    kim.playPrelude(CorridorsOfPower)

    kim.playPrelude(RiseToPower) {
          doTask("PartyDelegate<Scientists>")
          doTask("PartyDelegate<Unity>")
          doTask("PartyDelegate<Greens>")
        }
        .expect("3 ProjectCard, PartyLeader<Scientists>, PartyLeader<Unity>, PartyLeader<Greens>")
  }
}
