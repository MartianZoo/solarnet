package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class SearchForLifeTest : CardTest() {
  @Test
  internal fun `Scores three points when it has a science resource`() {
    newGame()
    engine.phase("Action")
    p1.manual("$SearchForLife, 1")
    p1.cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
    engine.phase("End")
    p1.assertCounts(23 to "VictoryPoint")
  }
}
