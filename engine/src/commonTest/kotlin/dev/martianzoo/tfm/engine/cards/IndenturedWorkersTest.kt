package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.BeforeTest
import kotlin.test.Test

class IndenturedWorkersTest : CardTest() {
  lateinit var p1: TfmGameplay

  @BeforeTest
  fun setUp() {
    newGame("BRM", 2)
    p1 = game.tfm(PLAYER1)

    with(p1) {
      phase("Action")
      sneak("100, 5 ProjectCard, 8 Heat")
    }
  }

  @Test
  fun indenturedWorkers() {
    with(p1) {
      playProject("IndenturedWorkers", 0)
      assertCounts(0 to "IndenturedWorkers") // just showing that its out of play

      // doing these things in between doesn't matter
      stdProject("AsteroidSP")
      stdAction("ConvertHeatSA")
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
      playProject("IndenturedWorkers", 0)

      manual("Generation") // use it or lose it!
      playProject("Soletta", 35)
    }
  }
}
