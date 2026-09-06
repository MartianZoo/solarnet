package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SearchForLifeTest : CardTest() {
  @Test
  internal fun `Reveals a project card before checking its microbe tag`() {
    newGame()
    engine.phase("Action")
    p1.manual("$SearchForLife, 1 MC")

    p1.cardAction1(SearchForLife) {
      p1.assertCounts(1 to "ProjectCard<Revealed>", 0 to "ProjectCard<Hand>")
      doTask("Science<$SearchForLife>")
    }

    p1.assertCounts(
        0 to "ProjectCard<Revealed>",
        0 to "ProjectCard<Hand>",
        1 to "Science<$SearchForLife>",
    )
  }

  @Test
  internal fun `Scores three points when it has a science resource`() {
    newGame()
    engine.phase("Action")
    p1.manual("$SearchForLife, 1 MC")
    p1.cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
    engine.manual("End FROM Phase")
    p1.assertCounts(23 to "VictoryPoint")
  }
}
