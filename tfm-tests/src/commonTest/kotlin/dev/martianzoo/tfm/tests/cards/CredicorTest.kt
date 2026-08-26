package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CredicorTest : CardTest() {
  @Test
  internal fun `Discounts both an expensive card and an expensive standard project`() {
    newGame()
    engine.phase("Action")
    p1.manual("40 MC, 2 ProjectCard, $CrediCor")
    p1.playProject(EarthCatapult, 23).expect("-19 MC")
    p1.stdProject("CitySP") { placeTile(2, 1) }.expect("-21 MC")
  }
}
