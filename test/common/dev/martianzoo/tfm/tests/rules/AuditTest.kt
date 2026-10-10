package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.Prelude1CardPack
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AuditTest : TfmTest() {
  @Test
  internal fun `one generic search records different tag criteria only in audited history`() {
    game = setUpGame(canonicalPremise(Prelude1CardPack))
    val p1 = game.testTfm(PLAYER1)
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
