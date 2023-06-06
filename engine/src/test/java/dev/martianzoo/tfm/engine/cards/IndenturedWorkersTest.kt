package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IndenturedWorkersTest {
  lateinit var game: Game
  lateinit var p1: TfmGameplay

  @BeforeEach
  fun setUp() {
    game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    p1 = game.tfm(PLAYER1)

    with(p1) {
      playCorp("Ecoline", 5)
      godMode().sneak("100, 8 Heat")
      phase("Action")
    }
  }

  @Test
  fun indenturedWorkers() {
    with(p1) {
      playProject("IndenturedWorkers")
      assertCounts(0 to "IndenturedWorkers") // just showing that its out of play

      // doing these things in between doesn't matter
      stdProject("AsteroidSP")
      stdAction("ConvertHeat")
      sellPatents(2)

      // we still have the discount
      playProject("Soletta", 35 - 8)

      // but no more
      playProject("AdvancedAlloys", 9)
    }
  }

  @Test
  fun indenturedWorkersGenerational() {
    with(p1) {
      playProject("IndenturedWorkers")

      godMode().manual("Generation") // use it or lose it!
      playProject("Soletta", 35)
    }
  }
}
