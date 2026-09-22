package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SearchForLifeTest : CardTest() {
  @Test
  internal fun `Records a successful external search result`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("$SearchForLife, 1 MC")

    p1.cardAction1(SearchForLife) {
      doTask("Science<$SearchForLife>")
    }

    p1.assertCounts(
        0 to "ProjectCard",
        1 to "Science<$SearchForLife>",
    )
  }

  @Test
  internal fun `Scores three points when it has a science resource`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("$SearchForLife, 1 MC")
    p1.cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
    admin.runOperation("End FROM Phase")
    p1.assertCounts(23 to "VictoryPoint")
  }
}
