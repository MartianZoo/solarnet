package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class IndenturedWorkersTest : CardTest() {
  @Test
  internal fun `Discounts the next card played`() {
    initializeGame("27, 2 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    p1.playProject(Soletta, 27).expect("-27")
  }

  @Test
  internal fun `Keeps its discount available through other actions`() {
    initializeGame("39, 4 ProjectCard, 8 Heat")
    p1.playProject(IndenturedWorkers, 0)
    p1.stdProject("AsteroidSP")
    p1.convertHeat()
    p1.sellPatents(2)
    p1.playProject(Soletta, 27).expect("-27")
  }

  @Test
  internal fun `Discounts only one card`() {
    initializeGame("36, 3 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    p1.playProject(Soletta, 27)
    p1.playProject(AdvancedAlloys, 9).expect("-9")
  }

  @Test
  internal fun `Expires at the end of the generation`() {
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
