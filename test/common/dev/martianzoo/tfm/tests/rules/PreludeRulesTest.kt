package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PreludeRulesTest : TfmGameplayTest() {
  @Test
  internal fun `A declared fizzle consumes a Prelude and allows the remaining Prelude to be played`() {
    newTestGame("PreludeExpansion", playerCount = 2)
    val checkpoint = game.timeline.checkpoint()

    // Follow mode accepts the player's declaration that their physical Prelude was unplayable.
    kim.inTurn { doTask("-PreludeCard") }.expect("15 MC, -PreludeCard")
    kim.playPrelude(DomeFarming).expect("$DomeFarming, -PreludeCard")

    kim.count("PreludeCard") shouldBe 0
    kim.auditGainsSince(checkpoint) shouldBe 1
  }

  // https://boardgamegeek.com/thread/3412262/i-bit-confused-on-combining-this-and-prelude-1-int
  @Test
  internal fun `Both Prelude packs share the same two-card setup and a single Prelude phase`() {
    newTestGame("PreludeExpansion, Prelude2CardPack", playerCount = 2)
    kim.count("PreludeCard") shouldBe 2
    stan.count("PreludeCard") shouldBe 2

    kim.playPrelude(Donation)
    kim.playPrelude(DomeFarming)
    stan.playPrelude(Supplier)
    stan.playPrelude(MetalsCompany)

    admin.count("ActionPhase") shouldBe 1
    kim.count("PreludeCard") shouldBe 0
    stan.count("PreludeCard") shouldBe 0
  }
}
