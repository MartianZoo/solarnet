package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.FakeSeptemTribus
import kotlin.test.Test

internal class FakeSeptemTribusTest : TfmSandboxTest() {
  @Test
  internal fun `Action ignores the chairman and pays once for each party with an owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion, FakeStuffBundle")
    kim.exMachina("$FakeSeptemTribus")
    kim.exMachina("-Chairman<Neutral>")
    kim.exMachina(
        "Chairman, PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, " +
            "PartyDelegate<Scientists>"
    )

    kim.cardAction1(FakeSeptemTribus).expect("4 MC")
  }
}
