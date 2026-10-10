package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StormcraftIncorporatedTest : TfmSandboxTest() {
  @Test
  internal fun `Can add a floater to another card`() {
    newTestGame(kimCorporation = StormcraftIncorporated)
    kim.exMachina("$TitanShuttles")

    kim.cardAction1(StormcraftIncorporated) { doTask("Floater<$TitanShuttles>") }
        .expect("Floater<$TitanShuttles>")
  }

  @Test
  internal fun `Can spend a floater as two heat for an action cost`() {
    newTestGame(kimCorporation = StormcraftIncorporated)
    kim.exMachina("Floater<$StormcraftIncorporated>, 6 Heat")

    kim.stdAction(
            "ConvertHeat",
            payment = {
              doTask("-Floater<$StormcraftIncorporated>")
              doTask("-6 Heat")
            },
        )
        .expect("-Floater<$StormcraftIncorporated>, -6 Heat, TemperatureStep")
  }

  @Test
  internal fun `Can spend no floaters on Local Heat Trapping`() {
    initializeStormcraftGame(floaters = 1, heat = 5)

    kim.playProject(LocalHeatTrapping, 1) {
          doTask("4 Plant")
          doTask("Ok")
        }
        .expect("-5 Heat, 4 Plant, 0 Floater<$StormcraftIncorporated>")
  }

  @Test
  internal fun `Can spend two floaters after Local Heat Trapping removes heat`() {
    initializeStormcraftGame(floaters = 2, heat = 5)

    kim.playProject(LocalHeatTrapping, 1) {
          doTask("4 Plant")
          doTask("-2 Floater THEN 4 Heat")
        }
        .expect("-2 Floater<$StormcraftIncorporated>, -Heat, 4 Plant")
  }

  @Test
  internal fun `Can spend three floaters before Local Heat Trapping removes heat`() {
    initializeStormcraftGame(floaters = 3, heat = 0)

    kim.playProject(LocalHeatTrapping, 1) {
          doTask("-3 Floater THEN 5 Heat")
          doTask("4 Plant")
        }
        .expect("-3 Floater<$StormcraftIncorporated>, 4 Plant")
  }

  private fun initializeStormcraftGame(floaters: Int, heat: Int) {
    newTestGame(kimCorporation = StormcraftIncorporated)
    kim.setToExMachina(floaters, "Floater<$StormcraftIncorporated>")
    kim.setToExMachina(heat, "Heat")
  }

  @Test
  internal fun `Floaters spend as heat without counting for Thermalist`() {
    newTestGame(playerCount = 2, kimCorporation = StormcraftIncorporated)
    kim.exMachina("9 Floater<$StormcraftIncorporated>")
    stan.setToExMachina(2, "Heat")
    kim.fundAward(cn("Thermalist"), 8)

    kim.stdAction(
            "ConvertHeat",
            payment = {
              doTask("-4 Floater<$StormcraftIncorporated>")
              declineTask()
            },
        )
        .expect("TemperatureStep, 0 Heat")

    victoryPoints() shouldBe listOf(21, 25)
  }
}
