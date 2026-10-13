package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
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
  internal fun `Eight heat raises temperature and terraform rating`() {
    newTestGame()
    kim.exMachina("8 Heat")

    kim.inTurn {
          doTasks("ConvertHeat", "-8 Heat")
          doTask("TemperatureStep")
          doTask("TerraformRating")
        }
        .expect("-8 Heat, TemperatureStep")
  }

  @Test
  internal fun `Temperature track bonuses are resolved during heat conversion`() {
    newTestGame()
    kim.exMachina("8 Heat, 2 TemperatureStep")

    kim.inTurn {
          doTasks("ConvertHeat", "-8 Heat")
          doTask("TemperatureStep")
          doTasks("TerraformRating", "PROD[Heat]")
        }
        .expect("-8 Heat, TemperatureStep")
  }

  @Test
  internal fun `Reaching zero degrees also places an ocean`() {
    newTestGame()
    kim.setToExMachina(8, "Heat")
    kim.setToExMachina(14, "TemperatureStep")

    kim.inTurn {
      doTasks(
          "ConvertHeat",
          "-8 Heat",
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
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTask("GreeneryTile<Tharsis_3_3>")
          selectTask("OxygenStep*")
          doTask("TerraformRating")
        }
        .expect("-8 Plant, OxygenStep")
  }

  @Test
  internal fun `An unaffordable adjacent area does not permit greenery fallback`() {
    newTestGame(addOptions = "HellasMap")
    kim.exMachina("NormalCityTile<Hellas_9_6>, 8 Plant")
    kim.setToExMachina(0, "MC")
    stan.exMachina("GreeneryTile<Hellas_9_5>, GreeneryTile<Hellas_8_5>, GreeneryTile<Hellas_8_6>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("ConvertPlants")
        selectTask("-X? Plant")
        doTask("-Plant / Owed<Class<Plant>>")
        selectOnlyTask()
        selectOnlyTask()
        doTask("GreeneryTile<Hellas_1_1>")
      }
    }
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("ConvertPlants")
        selectTask("-X? Plant")
        doTask("-Plant / Owed<Class<Plant>>")
        selectOnlyTask()
        selectOnlyTask()
        doTask("GreeneryTile<Hellas_9_7>")
        selectTask("OxygenStep*")
        selectTask("TerraformRating!")
        selectTask("OceanTile<WaterArea(HAS MAX 0 Tile)>*")
        doTask("OceanTile<Hellas_6_7>")
        selectTask("TerraformRating!")
        selectOnlyTask()
      }
    }
    kim.count("GreeneryTile") shouldBe 0

    kim.exMachina("6 MC")
    kim.inTurn {
      doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
      selectOnlyTask()
      doTask("GreeneryTile<Hellas_9_7>")
      selectTask("OxygenStep*")
      doTasks("-6 MC", "TerraformRating", "OceanTile<Hellas_6_7>", "TerraformRating")
    }
  }

  @Test
  internal fun `Greenery can still be placed after oxygen is maximized`() {
    newTestGame()
    kim.exMachina("8 Plant, 13 OxygenStep")
    stan.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_1_1>")
      selectTask("OxygenStep*")
      doTasks("2 Steel", "TerraformRating")
    }

    kim.inTurn {
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTask("GreeneryTile<Tharsis_3_3>")
          selectTask("OxygenStep*")
        }
        .expect("-8 Plant, 0 OxygenStep, 0 TerraformRating")
  }

  @Test
  internal fun `Reaching eight percent oxygen also raises temperature`() {
    newTestGame()
    kim.exMachina("8 Plant, 7 OxygenStep")

    kim.inTurn {
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTask("GreeneryTile<Tharsis_3_3>")
          selectTask("OxygenStep*")
          doTask("TerraformRating")
          selectTask("TemperatureStep*")
          doTask("TerraformRating")
        }
        .expect("-8 Plant, OxygenStep, TemperatureStep")
  }

  @Test
  internal fun `Tile placement grants both area and ocean adjacency bonuses`() {
    newTestGame()
    kim.exMachina("8 Plant, OceanTile<Tharsis_4_8>")

    kim.inTurn {
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTask("GreeneryTile<Tharsis_4_7>")
          selectTask("OxygenStep*")
          doTasks("Plant", "2 MC", "TerraformRating")
        }
        .expect("-7 Plant, OxygenStep")
  }

  @Test
  internal fun `Standard projects perform their advertised effects`() {
    newTestGame()
    kim.setToExMachina(100, "MC")

    kim.inTurn {
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
    }
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      selectTask("TemperatureStep*")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks(
          "UseStandardProject<AquiferProject>",
          "-18 MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "-25 MC",
          "PROD[MC]",
          "CityTile<Tharsis_4_4>",
          "Plant",
      )
    }

    kim.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_4_5>")
      selectTask("OxygenStep*")
      doTasks("2 Plant", "TerraformRating")
    }
  }

  @Test
  internal fun `A qualified player can claim a milestone`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")

    kim.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer35>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `A milestone cannot be claimed twice`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")
    stan.setToExMachina(35, "TerraformRating")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer35>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }

    shouldThrow<LimitsException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Terraformer35>>")
        selectTask("-X? MC")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }
  }

  @Test
  internal fun `Funding successive awards costs eight fourteen and twenty`() {
    newTestGame()

    kim.inTurn {
      doTasks("FundAward<Class<Landlord>>", "-8 MC")
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks("FundAward<Class<Scientist>>", "-14 MC")
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks("FundAward<Class<Thermalist>>", "-20 MC")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `A colony trade can be paid for with three energy`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.exMachina("3 Energy")

    kim.inTurn {
          doTasks("TradeAction<Action2>", "-Energy / Owed<Class<Energy>>")
          selectOnlyTask()
          doTasks("Trade<Ceres>", "2 Steel")
        }
        .expect("-3 Energy")
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Research cards cost three MC each`() {
      newTestGame(playerCount = 2, addOptions = "-VenusNextExpansion")
      kim.pass()
      stan.pass()

      kim.buyCards(3).expect("-9 MC, 3 ProjectCard")
      stan.buyCards(0)
      admin.count("Generation") shouldBe 2
    }

    @Test
    internal fun `Production converts existing energy before producing new resources`() {
      newTestGame(playerCount = 2, addOptions = "-VenusNextExpansion")
      kim.pass()
      stan.pass()
      kim.buyCards(0)
      stan.buyCards(0)
      kim.count("Energy") shouldBe 1
      kim.count("Heat") shouldBe 1

      stan.pass()
      kim.pass()
      kim.buyCards(0)
      stan.buyCards(0)

      kim.count("Energy") shouldBe 1
      kim.count("Heat") shouldBe 3
      kim.count("Steel") shouldBe 2
    }

    @Test
    internal fun `World Government terraforming gives no terraform rating`() {
      newTestGame(playerCount = 2)
      kim.pass()
      stan.pass()

      kim.wgt("TemperatureStep").expect("TemperatureStep, 0 TerraformRating")
    }
  }
}
