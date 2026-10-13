package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StormcraftIncorporatedTest : TfmSandboxTest() {
  @Test
  internal fun `Can add a floater to another card`() {
    newTestGame(kimCorporation = StormcraftIncorporated)
    kim.exMachina("$TitanShuttles")

    kim.inTurn {
      doTask("UseCardAction<StormcraftIncorporated, Action1>")
      selectTask("Floater.")
      doTask("Floater<TitanShuttles>")
    }
  }

  @Test
  internal fun `Can spend a floater as two heat for an action cost`() {
    newTestGame(kimCorporation = StormcraftIncorporated)
    kim.exMachina("Floater<$StormcraftIncorporated>, 6 Heat")

    kim.inTurn {
          doTasks("ConvertHeat", "-Floater<StormcraftIncorporated>", "-6 Heat")
          selectTask("MAX 0 Billing: TemperatureStep.")
          doTask("TerraformRating")
        }
        .expect("-Floater<$StormcraftIncorporated>, -6 Heat, TemperatureStep")
  }

  @Test
  internal fun `Can spend no floaters on Local Heat Trapping`() {
    initializeStormcraftGame(floaters = 1, heat = 5)

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LocalHeatTrapping>>",
              "-1 MC",
              "LocalHeatTrapping FROM ProjectCard",
              "-5 Heat",
              "4 Plant",
          )
          selectTask(
              "(-Floater<StormcraftIncorporated>! THEN 2 Heat!) OR (-2 Floater<StormcraftIncorporated>! THEN 4 Heat!) OR (-3 Floater<StormcraftIncorporated>! THEN 5 Heat!) OR Ok"
          )
          doTask("Ok")
          doTask("PlayedEvent<Class<LocalHeatTrapping>> FROM LocalHeatTrapping")
        }
        .expect("-5 Heat, 4 Plant, 0 Floater<$StormcraftIncorporated>")
  }

  @Test
  internal fun `Can spend two floaters after Local Heat Trapping removes heat`() {
    initializeStormcraftGame(floaters = 2, heat = 5)

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LocalHeatTrapping>>",
              "-1 MC",
              "LocalHeatTrapping FROM ProjectCard",
              "-5 Heat",
              "4 Plant",
          )
          selectTask(
              "(-Floater<StormcraftIncorporated>! THEN 2 Heat!) OR (-2 Floater<StormcraftIncorporated>! THEN 4 Heat!) OR (-3 Floater<StormcraftIncorporated>! THEN 5 Heat!) OR Ok"
          )
          doTasks("-2 Floater THEN 4 Heat", "4 Heat")
          doTask("PlayedEvent<Class<LocalHeatTrapping>> FROM LocalHeatTrapping")
        }
        .expect("-2 Floater<$StormcraftIncorporated>, -Heat, 4 Plant")
  }

  @Test
  internal fun `Can spend three floaters before Local Heat Trapping removes heat`() {
    initializeStormcraftGame(floaters = 3, heat = 0)

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LocalHeatTrapping>>",
              "-1 MC",
              "LocalHeatTrapping FROM ProjectCard",
              "-3 Floater THEN 5 Heat",
              "5 Heat",
              "-5 Heat",
              "4 Plant",
          )
          doTask("PlayedEvent<Class<LocalHeatTrapping>> FROM LocalHeatTrapping")
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
    kim.inTurn {
      doTasks("FundAward<Class<Thermalist>>", "-8 MC")
      selectTask("MAX 0 Billing: Thermalist!")
    }

    kim.inTurn {
          doTasks("ConvertHeat", "-4 Floater<StormcraftIncorporated>")
          selectTask("MAX 0 Billing: TemperatureStep.")
          doTask("TerraformRating")
          selectTask("-X Heat?")
          doTask("Ok")
        }
        .expect("TemperatureStep, 0 Heat")

    victoryPoints() shouldBe listOf(21, 25)
  }
}
