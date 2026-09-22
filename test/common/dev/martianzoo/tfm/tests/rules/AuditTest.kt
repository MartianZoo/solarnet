package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.exMachina
import dev.martianzoo.tfm.tests.TestOption.Prelude1CardPack
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

  @Test
  internal fun `one generic search records different tag criteria only in audited history`() {
    newGame(Prelude1CardPack)
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("SearchForCard<TagFilter<Class<SpaceTag>>>").expect("ProjectCard")
    p1.runOperation("SearchForCard<TagFilter<Class<PlantTag>>>").expect("ProjectCard")

    p1.count("SearchForCard<TagFilter<Class<SpaceTag>>>") shouldBe 0
    p1.count("SearchForCard<TagFilter<Class<PlantTag>>>") shouldBe 0
    p1.auditGainsSince(checkpoint) shouldBe 2
    game.events.changesSince(checkpoint).count {
      it.change.gaining?.type == p1.resolve("SearchForCard<TagFilter<Class<SpaceTag>>>")
    } shouldBe 1
    game.events.changesSince(checkpoint).count {
      it.change.gaining?.type == p1.resolve("SearchForCard<TagFilter<Class<PlantTag>>>")
    } shouldBe 1
  }
}
