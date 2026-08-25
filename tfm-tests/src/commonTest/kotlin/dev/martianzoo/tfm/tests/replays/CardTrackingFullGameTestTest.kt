package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.tests.cards.cardnames.AcquiredCompany
import dev.martianzoo.tfm.tests.cards.cardnames.AdaptedLichen
import dev.martianzoo.tfm.tests.cards.cardnames.AsteroidMining
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardTrackingFullGameTestTest : CardTrackingFullGameTest() {
  override val config = GameConfig("PreludeExpansion", "Player1")

  @Test
  internal fun namedDrawsPlaysAndDiscardsMaintainThePlayersHand() {
    p1.godMode().manual("3 ProjectCard") { p1.draw(AcquiredCompany, AdaptedLichen, AsteroidMining) }
    p1.cardsInHand shouldBe setOf(AcquiredCompany, AdaptedLichen, AsteroidMining)

    p1.godMode().manual("$AcquiredCompany FROM ProjectCard")
    p1.godMode().manual("-ProjectCard") { p1.discard(AdaptedLichen) }

    p1.cardsInHand shouldBe setOf(AsteroidMining)
    assertCardTrackingComplete()
  }
}
