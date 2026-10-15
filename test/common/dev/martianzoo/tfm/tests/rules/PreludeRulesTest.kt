package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
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
    kim.completeOperation { doTask("-PreludeCard") }.expect("15 MC, -PreludeCard")
    kim.completeOperation {
          doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")
          autoExecNow(EAGER)
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

    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")
    kim.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
    stan.autoExecNow(EAGER)

    admin.count("ActionPhase") shouldBe 1
    kim.count("PreludeCard") shouldBe 0
    stan.count("PreludeCard") shouldBe 0
  }
}
