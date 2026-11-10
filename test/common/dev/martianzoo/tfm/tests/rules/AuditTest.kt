package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AuditTest : TfmSandboxTest() {
  @Test
  internal fun `one generic search records different tag criteria only in audited history`() {
    newTestGame("prelude:cards")
    val checkpoint = game.events.nextOrdinal

    kim.runOperation("SearchForCard<TagFilter<Class<SpaceTag>>>").expect("ProjectCard")
    kim.runOperation("SearchForCard<TagFilter<Class<PlantTag>>>").expect("ProjectCard")

    kim.count("SearchForCard<TagFilter<Class<SpaceTag>>>") shouldBe 0
    kim.count("SearchForCard<TagFilter<Class<PlantTag>>>") shouldBe 0
    kim.auditGainsSince(checkpoint) shouldBe 2
    game.events.changesSince(checkpoint).count {
      it.change.gaining?.type == kim.resolve("SearchForCard<TagFilter<Class<SpaceTag>>>")
    } shouldBe 1
    game.events.changesSince(checkpoint).count {
      it.change.gaining?.type == kim.resolve("SearchForCard<TagFilter<Class<PlantTag>>>")
    } shouldBe 1
  }
}
