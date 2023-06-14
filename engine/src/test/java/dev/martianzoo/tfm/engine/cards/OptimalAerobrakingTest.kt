package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class OptimalAerobrakingTest {

  @Test
  fun optimalAerobraking() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))

    with(game.tfm(PLAYER1)) {
      phase("Action")

      godMode().sneak("5 ProjectCard, OptimalAerobraking, 14")
      assertCounts(14 to "Megacredit", 0 to "Heat")
      playProject("AsteroidCard", 14) {
        doTask("Ok") // but there's no one to steal from anyway TODO
      }
      assertCounts(3 to "Megacredit", 3 to "Heat")
    }
  }
}
