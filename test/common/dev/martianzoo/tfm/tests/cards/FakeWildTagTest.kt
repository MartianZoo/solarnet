package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeResearchCoordination
import dev.martianzoo.tfm.tests.cards.cardnames.FakeResearchNetwork
import dev.martianzoo.tfm.tests.cards.cardnames.MediaArchives
import dev.martianzoo.tfm.tests.cards.cardnames.SagittaFrontierServices
import dev.martianzoo.tfm.tests.fakeWildTags
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FakeWildTagTest : CardTest() {
  @Test
  internal fun `Fake wild tag stays inert on its card`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.runOperation("PreludeCard")
    admin.phase("Prelude")
    p1.startTurn()

    p1.playPrelude(FakeResearchNetwork).expect("PROD[1 MC], 3 ProjectCard, FakeWildTag")

    p1.count("FakeWildTag<$FakeResearchNetwork>") shouldBe 1
    p1.count("Tag<$FakeResearchNetwork>") shouldBe 0
  }

  @Test
  internal fun `An assigned wild Event tag does not count as a played event for Media Archives`() {
    newGame(PreludeExpansion, CorporateEraExpansion, FakeStuffBundle)
    p1.runOperation("50 MC, 4 ProjectCard")
    admin.phase("Action")
    p1.playProject(FakeResearchCoordination, 4)
    with(p1) {
      runOperation("${fakeWildTags("EventTag")}, NewTurn") {
            p1.count("EventTag") shouldBe 1
            playProject(MediaArchives, 8)
          }
          .expect("-8 MC")
    }
    p1.count("PlayedEvent") shouldBe 0
    p1.count("FakeWildTagUse") shouldBe 0
  }

  @Test
  internal fun `Sagitta rewards a wild-only card as tagless`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.playCorp(SagittaFrontierServices, 1)
    admin.phase("Action")

    p1.playProject(FakeResearchCoordination, 4).expect("0 MC")
  }
}
