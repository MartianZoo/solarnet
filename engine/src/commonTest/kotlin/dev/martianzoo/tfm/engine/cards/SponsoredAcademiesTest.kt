package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.tfm.canon.Canon.Option.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class SponsoredAcademiesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion)
    engine.phase("Action")
    engine.manual("9 Megacredit<Player1>, ProjectCard<Player1>, ProjectCard<Player2>")
  }

  @Test
  fun `with two cards in hand, plays Sponsored Academies`() {
    p1.manual("ProjectCard")
    p1.playProject("SponsoredAcademies", 9).expect("ProjectCard<Player1>, ProjectCard<Player2>")
  }

  @Test
  fun `with one card in hand, tries to play Sponsored Academies`() {
    shouldThrow<LimitsException> { p1.playProject("SponsoredAcademies", 9) }
  }
}
