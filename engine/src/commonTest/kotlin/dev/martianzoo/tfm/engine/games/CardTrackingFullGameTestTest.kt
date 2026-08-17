package dev.martianzoo.tfm.engine.games

import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.cardnames.AcquiredCompany
import dev.martianzoo.tfm.engine.cardnames.AdaptedLichen
import dev.martianzoo.tfm.engine.cardnames.AsteroidMining
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardTrackingFullGameTestTest : CardTrackingFullGameTest() {
  override val config = GameConfig("PreludeExpansion", "Player1")

  @Test
  fun namedDrawsPlaysAndDiscardsMaintainThePlayersHand() {
    p1.godMode().manual("3 ProjectCard") { p1.draw(AcquiredCompany, AdaptedLichen, AsteroidMining) }
    p1.cardsInHand shouldBe setOf(AcquiredCompany, AdaptedLichen, AsteroidMining)

    p1.godMode().manual("$AcquiredCompany FROM ProjectCard")
    p1.godMode().manual("-ProjectCard") { p1.discard(AdaptedLichen) }

    p1.cardsInHand shouldBe setOf(AsteroidMining)
    assertCardTrackingComplete()
  }
}
