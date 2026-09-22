package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.DomeFarming
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PreludeRulesTest : CardTest() {
  @Test
  internal fun `A normal Prelude may fizzle on the client's honor`() {
    newGame(PreludeExpansion)
    admin.phase("Prelude")
    val moneyBefore = p1.count("MC")
    val checkpoint = game.timeline.checkpoint()

    // Follow mode cannot prove that the physical Prelude was unplayable.
    p1.startTurn()
    p1.doTask("-PreludeCard")
    p1.startTurn()
    p1.playPrelude(DomeFarming)

    p1.assertCounts(1 to "$DomeFarming", 0 to "PreludeCard")
    p1.count("MC") shouldBe moneyBefore + 15
    p1.auditGainsSince(checkpoint) shouldBe 1
  }
}
