package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.PublicPlans
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PublicPlansTest : CardTest() {
  @Test
  internal fun `Cannot be played without another card to reveal`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 MC, ProjectCard")

    shouldThrow<AbstractException> { p1.playProject(PublicPlans, 7) }

    p1.assertCounts(7 to "MC", 1 to "ProjectCard", 0 to "$PublicPlans")
  }

  @Test
  internal fun `Rewards every revealed card and returns it to hand`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 MC, 3 ProjectCard")

    p1.playProject(PublicPlans, 7) {
          doTask("2 ProjectCard<Revealed FROM Hand>")
        }
        .expect("-5 MC, -ProjectCard")

    p1.assertCounts(
        2 to "ProjectCard<Hand>",
        0 to "ProjectCard<Revealed>",
        1 to "PlayedEvent<Class<$PublicPlans>>",
    )
  }
}
