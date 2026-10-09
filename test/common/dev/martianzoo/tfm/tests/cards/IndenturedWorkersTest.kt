package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class IndenturedWorkersTest : TfmSandboxTest() {
  @Test
  internal fun `Discounts the next project card played`() {
    newTestGame()
    kim.setToExMachina(27, "MC")

    kim.playProject(IndenturedWorkers, 0)
    kim.playProject(Soletta, 27).expect("-27 MC, PROD[7 Heat]")
  }

  @Test
  internal fun `Keeps its discount available through other actions`() {
    newTestGame()
    kim.setToExMachina(39, "MC")
    kim.setToExMachina(8, "Heat")

    kim.playProject(IndenturedWorkers, 0)
    kim.stdProject("AsteroidProject")
    kim.convertHeat()
    kim.sellPatents(2)
    kim.playProject(Soletta, 27).expect("-27 MC, PROD[7 Heat]")
  }

  @Test
  internal fun `Discounts only one project card`() {
    newTestGame()
    kim.setToExMachina(36, "MC")

    kim.playProject(IndenturedWorkers, 0)
    kim.playProject(Soletta, 27).expect("-27 MC, PROD[7 Heat]")
    kim.playProject(AdvancedAlloys, 9).expect("-9 MC")
  }

  @Test
  internal fun `Expires at the end of the generation`() {
    newTestGame()

    kim.playProject(IndenturedWorkers, 0)
    nextGeneration()
    kim.setToExMachina(35, "MC")

    kim.playProject(Soletta, 35).expect("-35 MC, PROD[7 Heat]")
  }

  @Test
  internal fun `Intervening Prelude leaves the discount for the next project card`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack")
    startActionPhase()
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>")

    kim.playProject(IndenturedWorkers, 0)
    kim.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      kim.playPrelude(Donation)
    }

    kim.playProject(Soletta, 27).expect("-27 MC, PROD[7 Heat]")
  }
}
