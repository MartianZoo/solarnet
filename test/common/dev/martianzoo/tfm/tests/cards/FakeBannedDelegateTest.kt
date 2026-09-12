package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeBannedDelegate
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FakeBannedDelegateTest : CardTest() {
  @Test
  internal fun `returns a selected non-leader delegate to its owner's reserve`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    p2.runOperation(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate"
    )
    val reserveBefore = p2.count("ReserveDelegate")

    p1.runOperation("$FakeBannedDelegate") {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("ReserveDelegate<Player2> FROM PartyDelegate<MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("ReserveDelegate") shouldBe reserveBefore + 1
  }
}
