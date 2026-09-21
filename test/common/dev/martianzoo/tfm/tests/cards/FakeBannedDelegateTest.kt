package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeBannedDelegate
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FakeBannedDelegateTest : CardTest() {
  @Test
  internal fun `removes a selected non-leader delegate`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")
    val delegatesBefore = p2.count("Delegate")

    p1.runOperation("$FakeBannedDelegate") {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("-PartyDelegate<MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("Delegate") shouldBe delegatesBefore - 1
  }
}
