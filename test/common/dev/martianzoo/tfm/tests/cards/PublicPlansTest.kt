package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.PublicPlans
import kotlin.test.Test

internal class PublicPlansTest : CardTest() {
  @Test
  internal fun `Can be played without another card to reveal`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")

    p1.playProject(PublicPlans, 7)

    p1.assertCounts(0 to "MC", 0 to "ProjectCard", 1 to "PlayedEvent<Class<$PublicPlans>>")
  }

  @Test
  internal fun `Rewards the declared number of cards without moving them`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, 3 ProjectCard")

    p1.playProject(PublicPlans, 7) {
          doTask("2 MC")
        }
        .expect("-5 MC, -ProjectCard")

    p1.assertCounts(
        2 to "ProjectCard",
        1 to "PlayedEvent<Class<$PublicPlans>>",
    )
  }
}
