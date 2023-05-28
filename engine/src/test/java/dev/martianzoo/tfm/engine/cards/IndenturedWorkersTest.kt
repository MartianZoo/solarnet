package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.execapi.PlayerSession.Companion.session
import dev.martianzoo.tfm.execapi.TerraformingMars.phase
import dev.martianzoo.tfm.execapi.TerraformingMars.playCard
import dev.martianzoo.tfm.execapi.TerraformingMars.playCorp
import dev.martianzoo.tfm.execapi.TerraformingMars.sellPatents
import dev.martianzoo.tfm.execapi.TerraformingMars.stdAction
import dev.martianzoo.tfm.execapi.TerraformingMars.stdProject
import org.junit.jupiter.api.Test

class IndenturedWorkersTest {

  @Test
  fun indenturedWorkers() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Ecoline", 5)
      writer.unsafe().sneak("100, 8 Heat")

      phase("Action")
      playCard("IndenturedWorkers")
      assertCounts(0 to "IndenturedWorkers") // just showing that its out of play

      // doing these things in between doesn't matter
      stdProject("AsteroidSP")
      stdAction("ConvertHeat")
      sellPatents(2)

      // we still have the discount
      playCard("Soletta", 35 - 8)

      // but no more
      playCard("AdvancedAlloys", 9)
    }
  }

  @Test
  fun indenturedWorkersGenerational() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Ecoline", 5)
      writer.unsafe().sneak("100")

      phase("Action")
      playCard("IndenturedWorkers")

      operation("Generation") // use it or lose it!
      playCard("Soletta", 35)
    }
  }
}
