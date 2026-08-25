package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class OptimalAerobrakingTest : CardTest() {

  @Test
  internal fun `Pays when its owner plays an asteroid event`() {
    newGame()
    engine.phase("Action")
    p1.manual("ProjectCard, $OptimalAerobraking, 14")
    p1.playProject(AsteroidCard, 14).expect("-11, 3 Heat")
  }
}
