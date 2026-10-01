package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.PublicPlans
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PublicPlansTest : CardTest() {
  @Test
  internal fun `Rewards two revealed cards without moving them`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, 3 ProjectCard")

    p1.playProject(PublicPlans, 7) {
          doTask("MC")
        }
        .expect("-5 MC, -ProjectCard")

    p1.assertCounts(
        2 to "ProjectCard",
        1 to "PlayedEvent<Class<$PublicPlans>>",
    )
  }

  @Test
  internal fun `Revealing the only other card earns one MC`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, 2 ProjectCard")

    p1.playProject(PublicPlans, 7).expect("-6 MC, -ProjectCard")
    p1.assertCounts(1 to "ProjectCard", 1 to "PlayedEvent<Class<$PublicPlans>>")
  }

  @Test
  internal fun `May reveal only one of several other cards`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, 3 ProjectCard")

    p1.playProject(PublicPlans, 7) { declineTask() }.expect("-6 MC, -ProjectCard")
    p1.assertCounts(2 to "ProjectCard", 1 to "PlayedEvent<Class<$PublicPlans>>")
  }

  @Test
  internal fun `Public Plans cannot be played as the last card in hand`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(PublicPlans, 7) }
    p1.assertCounts(7 to "MC", 1 to "ProjectCard", 0 to "PlayedEvent<Class<$PublicPlans>>")
  }
}
