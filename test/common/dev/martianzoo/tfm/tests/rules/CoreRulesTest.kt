package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CoreRulesTest : TfmSandboxTest() {
  @Test
  internal fun `Selling patents returns one mc per card`() {
    newTestGame()
    kim.setToExMachina(3, "ProjectCard")

    kim.inTurn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "2! MC FROM ProjectCard")
    }
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newTestGame()

    shouldThrow<PetSyntaxException> {
      kim.inTurn {
        doTasks(*standardProjectTasks("SellPatentsProject"))
        doTask("0 MC FROM ProjectCard!")
      }
    }
  }

  @Test
  internal fun `Temperature track bonuses are resolved during heat conversion`() {
    newTestGame()
    kim.exMachina("8 Heat, 2 TemperatureStep")

    kim.inTurn {
      doTasks("UseAction<ConvertHeatAction, Action1>", "8 Pay<Class<Heat>> FROM Heat")
      doTask("TemperatureStep")
      doTasks("TerraformRating", "PROD[Heat]")
    }
  }

  @Test
  internal fun `Reaching zero degrees also places an ocean`() {
    newTestGame()
    kim.setToExMachina(8, "Heat")
    kim.setToExMachina(14, "TemperatureStep")

    kim.inTurn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }
  }

  @Test
  internal fun `Eight plants place greenery and raise oxygen and terraform rating`() {
    newTestGame()
    kim.exMachina("8 Plant")

    kim.inTurn {
      doTask("UseAction<ConvertPlantsAction, Action1>")
      // The placement waits while the Player pays; it must neither execute nor block payment.
      shouldThrow<TaskException> { doTask("GreeneryTile<Tharsis_3_3>") }
      doTask("8 Pay<Class<Plant>> FROM Plant")
      doTask("GreeneryTile<Tharsis_3_3>")
      doTask("OxygenStep")
      doTask("TerraformRating")
    }
  }

  @Test
  internal fun `An unaffordable adjacent area does not permit greenery fallback`() {
    newTestGame(addOptions = "HellasMap")
    kim.exMachina("NormalCityTile<Hellas_9_6>, 8 Plant")
    kim.setToExMachina(0, "MC")
    stan.exMachina("GreeneryTile<Hellas_9_5>, GreeneryTile<Hellas_8_5>, GreeneryTile<Hellas_8_6>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("UseAction<ConvertPlantsAction, Action1>")
        doTask("8 Pay<Class<Plant>> FROM Plant")
        doTask("GreeneryTile<Hellas_1_1>")
      }
    }
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseAction<ConvertPlantsAction, Action1>")
        doTask("8 Pay<Class<Plant>> FROM Plant")
        doTask("GreeneryTile<Hellas_9_7>")
        doTask("OxygenStep")
        doTask("TerraformRating")
        doTask("OceanTile<Hellas_6_7>")
        doTask("TerraformRating")
        // Resolve the mandatory Hellas placement bill to expose the unaffordable cost.
        doTask("-6 MC")
      }
    }
    kim.count("GreeneryTile") shouldBe 0

    kim.exMachina("6 MC")
    kim.inTurn {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTask("GreeneryTile<Hellas_9_7>")
      doTask("OxygenStep")
      doTasks("-6 MC", "TerraformRating", "OceanTile<Hellas_6_7>", "TerraformRating")
    }
  }

  @Test
  internal fun `Greenery can still be placed after oxygen is maximized`() {
    newTestGame()
    kim.exMachina("8 Plant, 13 OxygenStep")
    stan.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_1_1>",
      )
      doTask("OxygenStep")
      doTasks("2 Steel", "TerraformRating")
    }

    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_3_3>")
          doTask("OxygenStep")
        }
        .expect("-8 Plant, 0 OxygenStep, 0 TerraformRating")
  }

  @Test
  internal fun `Reaching eight percent oxygen also raises temperature`() {
    newTestGame()
    kim.exMachina("8 Plant, 7 OxygenStep")

    kim.inTurn {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTask("GreeneryTile<Tharsis_3_3>")
      doTask("OxygenStep")
      doTask("TerraformRating")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
  }

  @Test
  internal fun `Tile placement grants both area and ocean adjacency bonuses`() {
    newTestGame()
    kim.exMachina("8 Plant, OceanTile<Tharsis_4_8>")

    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_4_7>")
          doTask("OxygenStep")
          doTasks("Plant", "2 MC", "TerraformRating")
        }
        .expect("-7 Plant, OxygenStep")
  }

  @Test
  internal fun `Standard projects perform their advertised effects`() {
    newTestGame()
    kim.setToExMachina(100, "MC")

    kim.inTurn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
    kim.inTurn {
      doTasks(*standardProjectTasks("AsteroidProject"), "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }

    kim.inTurn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_4_4>",
          "Plant",
      )
    }

    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_4_5>",
      )
      doTask("OxygenStep")
      doTasks("2 Plant", "TerraformRating")
    }
  }

  @Test
  internal fun `A player who passed before the action phase cannot act in it`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 2)
    // As when Red Appeasement is played through Head Start during the Prelude phase.
    kim.exMachina("Pass")

    advanceUntilActionPhase()

    shouldThrow<TaskException> { kim.inTurn { doTask("Pass") } }
    stan.inTurn { doTask("Pass") }
  }
}
