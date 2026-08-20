package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class IndenturedWorkersTest : CardTest() {
  @Test
  fun `after Indentured Workers, plays a discounted card`() {
    initializeGame("27, 2 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    p1.playProject(Soletta, 27).expect("-27")
  }

  @Test
  fun `after other actions, uses the Indentured Workers discount`() {
    initializeGame("39, 4 ProjectCard, 8 Heat")
    p1.playProject(IndenturedWorkers, 0)
    p1.stdProject("AsteroidSP")
    p1.stdAction("ConvertHeatSA")
    p1.sellPatents(2)
    p1.playProject(Soletta, 27).expect("-27")
  }

  @Test
  fun `after using the Indentured Workers discount, plays another card`() {
    initializeGame("36, 3 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    p1.playProject(Soletta, 27)
    p1.playProject(AdvancedAlloys, 9).expect("-9")
  }

  @Test
  fun `after the generation ends, plays a card after Indentured Workers`() {
    initializeGame("35, 2 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    engine.manual("Generation")
    p1.playProject(Soletta, 35).expect("-35")
  }

  private fun initializeGame(instruction: String) {
    newGame()
    engine.phase("Action")
    p1.manual(instruction)
  }
}
