package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CoreRulesTest : TfmSandboxTest() {
  @Test
  internal fun `Selling patents returns one mc per card`() {
    newTestGame()
    kim.setToExMachina(3, "ProjectCard")

    kim.inTurn {
      doTasks("UseStandardProject<SellPatentsProject>", "2! MC FROM ProjectCard")
    }
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newTestGame()

    shouldThrow<PetSyntaxException> {
      kim.inTurn {
        doTask("UseStandardProject<SellPatentsProject>")
        doTask("0 MC FROM ProjectCard!")
      }
    }
  }

  @Test
  internal fun `Eight heat raises temperature and terraform rating`() {
    newTestGame()
    kim.exMachina("8 Heat")

    kim.inTurn {
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
  }

  @Test
  internal fun `Temperature track bonuses are resolved during heat conversion`() {
    newTestGame()
    kim.exMachina("8 Heat, 2 TemperatureStep")

    kim.inTurn {
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat")
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
          "ConvertHeat",
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
      doTask("ConvertPlants")
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
        doTask("ConvertPlants")
        doTask("8 Pay<Class<Plant>> FROM Plant")
        doTask("GreeneryTile<Hellas_1_1>")
      }
    }
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("ConvertPlants")
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
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
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
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_1_1>",
      )
      doTask("OxygenStep")
      doTasks("2 Steel", "TerraformRating")
    }

    kim.inTurn {
          doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
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
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
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
          doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
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
      doTasks("UseStandardProject<PowerPlantProject>", "11 Pay<> FROM MC", "PROD[Energy]")
    }
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks(
          "UseStandardProject<AquiferProject>",
          "18 Pay<> FROM MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_4_4>",
          "Plant",
      )
    }

    kim.inTurn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_4_5>",
      )
      doTask("OxygenStep")
      doTasks("2 Plant", "TerraformRating")
    }
  }

  @Test
  internal fun `A qualified player can claim a milestone`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")

    kim.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer35>>", "8 Pay<> FROM MC")
      doTask("Terraformer35")
    }
  }

  @Test
  internal fun `A milestone cannot be claimed twice`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")
    stan.setToExMachina(35, "TerraformRating")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer35>>", "8 Pay<> FROM MC")
      doTask("Terraformer35")
    }

    shouldThrow<LimitsException> {
      stan.inTurn {
        doTask("ClaimMilestone<Class<Terraformer35>>")
        doTask("8 Pay<> FROM MC")
        doTask("Terraformer35")
      }
    }
  }

  @Test
  internal fun `Funding successive awards costs eight fourteen and twenty`() {
    newTestGame()

    kim.inTurn {
      doTasks("FundAward<Class<Landlord>>", "8 Pay<> FROM MC")
      doTask("Landlord")
    }
    kim.inTurn {
      doTasks("FundAward<Class<Scientist>>", "14 Pay<> FROM MC")
      doTask("Scientist")
    }
    kim.inTurn {
      doTasks("FundAward<Class<Thermalist>>", "20 Pay<> FROM MC")
      doTask("Thermalist")
    }
  }

  @Test
  internal fun `A colony trade can be paid for with three energy`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.exMachina("3 Energy")

    kim.inTurn {
      doTasks("TradeAction<Action2>", "3 Pay<Class<Energy>> FROM Energy")
      doTasks("Trade<Ceres>", "2 Steel")
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

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Research cards cost three MC each`() {
      newTestGame(playerCount = 2, addOptions = "-VenusNextExpansion")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTasks("-1 ProjectCard<Selecting>").expect("-9 MC, 3 ProjectCard")
      stan.doTask("-4 ProjectCard<Selecting>")
      admin.count("Generation") shouldBe 2
    }

    @Test
    internal fun `Production converts existing energy before producing new resources`() {
      newTestGame(playerCount = 2, addOptions = "-VenusNextExpansion")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")
      kim.count("Energy") shouldBe 1
      kim.count("Heat") shouldBe 1

      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")

      kim.count("Energy") shouldBe 1
      kim.count("Heat") shouldBe 3
      kim.count("Steel") shouldBe 2
    }

    @Test
    internal fun `World Government terraforming gives no terraform rating`() {
      newTestGame(playerCount = 2)
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 TerraformRating")
    }
  }
}
