package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class OptimalAerobrakingTest {

  @Test
  fun optimalAerobraking() {
    val game = setUpGame("BRHXP", 2)

    with(game.tfm(PLAYER1)) {
      phase("Action")

      godMode().sneak("5 ProjectCard, OptimalAerobraking, 14")
      assertCounts(14 to "Megacredit", 0 to "Heat")
      playProject("AsteroidCard", 14) { doTask("Ok") }
      assertCounts(3 to "Megacredit", 3 to "Heat")
    }
  }
}
