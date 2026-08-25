package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SponsoredAcademiesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion, players = 3)
    engine.phase("Action")
    engine.manual(
        "9 Megacredit<Player1>, ProjectCard<Player1>, ProjectCard<Player2>, ProjectCard<Player3>"
    )
  }

  @Test
  internal fun `Owner discards one and draws two while every opponent draws one`() {
    p1.manual("ProjectCard")

    p1.playProject(SponsoredAcademies, 9)
        .expect("ProjectCard<Player1>, ProjectCard<Player2>, ProjectCard<Player3>")
  }

  @Test
  internal fun `Cannot be played with only one card in hand`() {
    shouldThrow<LimitsException> { p1.playProject(SponsoredAcademies, 9) }
  }
}
