package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.execapi.PlayerSession.Companion.session
import org.junit.jupiter.api.Test

class OptimalAerobrakingTest {

  @Test
  fun optimalAerobraking() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))

    with(game.session(PLAYER1)) {
      operation("5 ProjectCard, OptimalAerobraking")
      assertCounts(0 to "Megacredit", 0 to "Heat")
      operation("AsteroidCard", "Ok") // TODO infer this??
      assertCounts(3 to "Megacredit", 3 to "Heat")
    }
  }
}
