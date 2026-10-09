package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SearchForLifeTest : CardTest() {
  @Test
  internal fun `Science added outside the search does not claim a revealed card`() {
    newGame()
    p1.runOperation("$SearchForLife")

    val checkpoint = game.timeline.checkpoint()
    p1.runOperation("Science<$SearchForLife>")

    p1.count("Science<$SearchForLife>") shouldBe 1
    p1.auditGainsSince(checkpoint) shouldBe 0
  }

  @Test
  internal fun `Records a successful external search result`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("$SearchForLife, 1 MC")

    val checkpoint = game.timeline.checkpoint()
    val result =
        p1.cardAction1(SearchForLife) {
          doTask("ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>")
        }

    result.changes
        .filter { it.change.gaining?.type == p1.resolve("ProjectCard<Revealed>") }
        .sumOf { it.change.count } shouldBe 1

    p1.assertCounts(
        0 to "ProjectCard",
        0 to "ProjectCard<Revealed>",
        1 to "Science<$SearchForLife>",
    )
    p1.auditGainsSince(checkpoint) shouldBe 1
  }

  @Test
  internal fun `Discards a revealed card without a Microbe tag`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("$SearchForLife, 1 MC")

    val checkpoint = game.timeline.checkpoint()
    p1.cardAction1(SearchForLife) { doTask("Ok") }

    p1.assertCounts(0 to "ProjectCard<Revealed>", 0 to "Science<$SearchForLife>")
    p1.auditGainsSince(checkpoint) shouldBe 0
  }
}
