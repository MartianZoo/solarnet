package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SponsoredAcademiesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion, players = 3)
    admin.phase("Action")
    admin.runOperation(
        "9 MC<Player1>, ProjectCard<Player1>, ProjectCard<Player2>, ProjectCard<Player3>"
    )
  }

  @Test
  internal fun `Point Luna draw supplies the mandatory discard when Sponsored Academies is the only hand card`() {
    newGame(VenusNextExpansion, PreludeExpansion, players = 3)
    p1.playCorp(PointLuna, 0)
    admin.phase("Action")
    p1.count("ProjectCard") shouldBe 1

    p1.playProject(SponsoredAcademies, 9)
        .expect("2 ProjectCard<Player1>, ProjectCard<Player2>, ProjectCard<Player3>")
  }

  @Test
  internal fun `Cannot be played with only one card in hand`() {
    shouldThrow<LimitsException> { p1.playProject(SponsoredAcademies, 9) }
  }
}
