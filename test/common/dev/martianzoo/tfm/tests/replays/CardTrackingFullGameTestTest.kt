package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.cards.cardnames.AcquiredCompany
import dev.martianzoo.tfm.tests.cards.cardnames.AdaptedLichen
import dev.martianzoo.tfm.tests.cards.cardnames.AsteroidMining
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardTrackingFullGameTestTest : CardTrackingFullGameTest() {
  override val config = GameConfig("PreludeExpansion", "Player1")

  @Test
  internal fun namedDrawsReturnsPlaysAndDiscardsMaintainThePlayersHand() {
    p1.manual("3 ProjectCard") { p1.draw(AcquiredCompany, AdaptedLichen, AsteroidMining) }
    p1.cardsInHand shouldBe setOf(AcquiredCompany, AdaptedLichen, AsteroidMining)

    p1.manual("$AcquiredCompany FROM ProjectCard")
    shouldThrow<IllegalStateException> { p1.draw(AcquiredCompany) }
    p1.manual("ProjectCard") { p1.returnToHand(AcquiredCompany) }
    p1.manual("-2 ProjectCard") { p1.discard(AcquiredCompany, AdaptedLichen) }

    p1.cardsInHand shouldBe setOf(AsteroidMining)
    assertCardTrackingComplete()
  }
}
