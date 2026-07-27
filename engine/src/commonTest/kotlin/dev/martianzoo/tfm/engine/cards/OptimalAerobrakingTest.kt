package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class OptimalAerobrakingTest : CardTest() {

  @Test
  fun `with Optimal Aerobraking, plays an asteroid`() {
    newGame()
    engine.phase("Action")
    p1.manual("ProjectCard, OptimalAerobraking, 14")
    p1.playProject("AsteroidCard", 14) { doTask("Ok") }.expect("-11, 3 Heat")
  }
}
