package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeSeptemTribus
import kotlin.test.Test

internal class FakeSeptemTribusTest : CardTest() {
  @Test
  internal fun `Action pays once for each party containing an owned delegate`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    p1.manual("$FakeSeptemTribus")
    p1.manual(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<Scientists> FROM ReserveDelegate"
    )
    admin.phase("Action")

    p1.cardAction1(FakeSeptemTribus).expect("4 MC")
  }
}
