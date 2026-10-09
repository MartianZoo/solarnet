package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class IndenturedWorkersTest : CardTest() {
  @Test
  internal fun `Discounts the next project card played`() {
    initializeGame("27 MC, 2 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    p1.playProject(Soletta, 27).expect("-27 MC")
  }

  @Test
  internal fun `Keeps its discount available through other actions`() {
    initializeGame("39 MC, 4 ProjectCard, 8 Heat")
    p1.playProject(IndenturedWorkers, 0)
    p1.stdProject("AsteroidProject")
    p1.convertHeat()
    p1.sellPatents(2)
    p1.playProject(Soletta, 27).expect("-27 MC")
  }

  @Test
  internal fun `Discounts only one project card`() {
    initializeGame("36 MC, 3 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    p1.playProject(Soletta, 27)
    p1.playProject(AdvancedAlloys, 9).expect("-9 MC")
  }

  @Test
  internal fun `Expires at the end of the generation`() {
    initializeGame("35 MC, 2 ProjectCard")
    p1.playProject(IndenturedWorkers, 0)
    admin.runOperation("Generation")
    p1.playProject(Soletta, 35).expect("-35 MC")
  }

  @Test
  internal fun `Intervening Prelude leaves the discount for the next project card`() {
    newGame(PreludeExpansion, Prelude2CardPack, CorporateEraExpansion)
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")
    p1.runOperation("50 MC, 2 ProjectCard")

    p1.playProject(IndenturedWorkers, 0)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Donation)
    }
    val moneyBefore = p1.count("MC")

    p1.playProject(Soletta, 27).expect("PROD[7 Heat]")
    p1.count("MC") shouldBe moneyBefore - 27
  }

  private fun initializeGame(instruction: String) {
    newGame()
    admin.phase("Action")
    p1.runOperation(instruction)
  }
}
