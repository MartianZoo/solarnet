package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PreludeRulesTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `A declared fizzle consumes a Prelude and allows the remaining Prelude to be played`() {
    newTestGame("PreludeExpansion", playerCount = 2)
    val checkpoint = game.events.nextOrdinal

    // Follow mode accepts the player's declaration that their physical Prelude was unplayable.
    kim.completeOperation { doTasks("-PreludeCard", "Audit", "15 MC") }
        .expect("15 MC, -PreludeCard")
    kim.completeOperation {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
              "$DomeFarming FROM PreludeCard",
              "PROD[Plant]",
              "PROD[2 MC]",
          )
        }
        .expect("$DomeFarming, -PreludeCard")

    kim.count("PreludeCard") shouldBe 0
    kim.auditGainsSince(checkpoint) shouldBe 1
  }

  // https://boardgamegeek.com/thread/3412262/i-bit-confused-on-combining-this-and-prelude-1-int
  @Test
  internal fun `Both Prelude packs share the same two-card setup and a single Prelude phase`() {
    newTestGame("PreludeExpansion, prelude2:cards", playerCount = 2)
    kim.count("PreludeCard") shouldBe 2
    stan.count("PreludeCard") shouldBe 2

    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "$Donation FROM PreludeCard",
        "21 MC",
    )
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
        "$DomeFarming FROM PreludeCard",
        "PROD[Plant]",
        "PROD[2 MC]",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
        "$Supplier FROM PreludeCard",
        "PROD[2 Energy]",
        "4 Steel",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
        "$MetalsCompany FROM PreludeCard",
        "PROD[MC]",
        "PROD[Steel]",
        "PROD[Titanium]",
    )

    admin.count("ActionPhase") shouldBe 1
    kim.count("PreludeCard") shouldBe 0
    stan.count("PreludeCard") shouldBe 0
  }
}
