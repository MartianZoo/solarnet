package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.SeptemTribus
import kotlin.test.Test

internal class SeptemTribusTest : TfmSandboxTest() {
  @Test
  internal fun `Action ignores the chairman and pays once for each party with an owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$SeptemTribus")
    kim.exMachina("-Chairman<Neutral>")
    kim.exMachina(
        "Chairman, PartyFollower<MarsFirst>, PartyLeader<MarsFirst>, " + "PartyLeader<Scientists>"
    )

    kim.inTurn {
      doTask("UseCardAction<SeptemTribus, Action1>")
      doTask("4 MC")
    }
  }
}
