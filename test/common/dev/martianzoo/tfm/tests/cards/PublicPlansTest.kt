package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.cards.cardnames.PublicPlans
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PublicPlansTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Rewards two revealed cards and returns them to hand`() {
    kim.setToExMachina(3, "ProjectCard")

    kim.playProject(PublicPlans, 7) {
          doTask("2 ProjectCard<Revealed FROM Hand>")
        }
        .expect("-5 MC, -ProjectCard")
  }

  @Test
  internal fun `Can reveal only one of several other cards`() {
    kim.setToExMachina(3, "ProjectCard")

    kim.playProject(PublicPlans, 7) { doTask("ProjectCard<Revealed FROM Hand>") }
        .expect("-6 MC, -ProjectCard")
  }

  @Test
  internal fun `Cannot be played as the last card in hand`() {
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<NotNowException> {
      kim.playProject(PublicPlans, 7) { doTask("ProjectCard<Revealed FROM Hand>") }
    }
  }
}
