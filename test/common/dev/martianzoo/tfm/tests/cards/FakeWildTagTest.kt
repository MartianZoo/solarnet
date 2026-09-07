package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.FakeCardsCardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeResearchNetwork
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FakeWildTagTest : CardTest() {
  @Test
  internal fun `Fake wild tag stays inert on its card`() {
    newGame(PreludeExpansion, FakeCardsCardPack)
    p1.manual("PreludeCard")
    admin.phase("Prelude")
    p1.startTurn()

    p1.playPrelude(FakeResearchNetwork).expect("PROD[1 MC], 3 ProjectCard, FakeWildTag")

    p1.count("FakeWildTag<$FakeResearchNetwork>") shouldBe 1
    p1.count("Tag<$FakeResearchNetwork>") shouldBe 0
  }
}
