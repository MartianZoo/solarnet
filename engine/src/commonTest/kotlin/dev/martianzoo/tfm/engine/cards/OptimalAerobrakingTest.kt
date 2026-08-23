package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
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
