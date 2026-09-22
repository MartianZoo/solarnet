package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.exMachina
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AuditTest : CardTest() {
  @Test
  internal fun `exMachina records a transient Audit for the adjusting player`() {
    newGame()
    val checkpoint = game.timeline.checkpoint()

    agents.exMachina(p1.actor, "MC")

    p1.count("MC") shouldBe 1
    p1.count("Audit") shouldBe 0
    p1.auditGainsSince(checkpoint) shouldBe 1
  }
}
