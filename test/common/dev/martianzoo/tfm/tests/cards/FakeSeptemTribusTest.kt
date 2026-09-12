package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeSeptemTribus
import kotlin.test.Test

internal class FakeSeptemTribusTest : CardTest() {
  @Test
  internal fun `Action ignores the chairman and pays once for each party with an owned delegate`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    p1.runOperation("$FakeSeptemTribus")
    admin.runOperation("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.runOperation(
        "Chairman FROM ReserveDelegate, PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<Scientists> FROM ReserveDelegate"
    )
    admin.phase("Action")

    p1.cardAction1(FakeSeptemTribus).expect("4 MC")
  }
}
