package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class CredicorTest : CardTest() {
  @Test
  internal fun `Discounts both an expensive card and an expensive standard project`() {
    newGame()
    engine.phase("Action")
    p1.manual("40, 2 ProjectCard, $CrediCor")
    p1.playProject(EarthCatapult, 23).expect("-19")
    p1.stdProject("CitySP") { placeTile(2, 1) }.expect("-21")
  }
}
