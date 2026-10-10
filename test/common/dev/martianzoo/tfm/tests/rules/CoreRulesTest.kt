package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CoreRulesTest : TfmSandboxTest() {
  @Test
  internal fun `Selling patents returns one mc per card`() {
    newTestGame()
    kim.setToExMachina(3, "ProjectCard")

    kim.sellPatents(2).expect("-2 ProjectCard, 2 MC")
  }

  @Test
  internal fun `Eight heat raises temperature and terraform rating`() {
    newTestGame()
    kim.exMachina("8 Heat")

    kim.convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating")
  }

  @Test
  internal fun `Temperature track bonuses are resolved during heat conversion`() {
    newTestGame()
    kim.exMachina("8 Heat, 2 TemperatureStep")

    kim.convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, PROD[Heat]")
  }

  @Test
  internal fun `Reaching zero degrees also places an ocean`() {
    newTestGame()
    kim.exMachina("8 Heat, 14 TemperatureStep")

    kim.convertHeat { doTask("OceanTile<Tharsis_1_2>") }
        .expect("-8 Heat, TemperatureStep, OceanTile, 2 TerraformRating")
  }

  @Test
  internal fun `Eight plants place greenery and raise oxygen and terraform rating`() {
    newTestGame()
    kim.exMachina("8 Plant")

    kim.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `An unaffordable adjacent area does not permit greenery fallback`() {
    newTestGame(addOptions = "HellasMap")
    kim.exMachina("NormalCityTile<Hellas_9_6>, 8 Plant")
    kim.setToExMachina(0, "MC")
    stan.exMachina("GreeneryTile<Hellas_9_5>, GreeneryTile<Hellas_8_5>, GreeneryTile<Hellas_8_6>")

    shouldThrow<NarrowingException> {
      kim.convertPlants { doTask("GreeneryTile<Hellas_1_1>") }
    }
    shouldThrow<LimitsException> {
      kim.convertPlants {
        doTask("GreeneryTile<Hellas_9_7>")
        doTask("OceanTile<Hellas_6_7>")
        doTask("-6 MC")
      }
    }
    kim.count("GreeneryTile") shouldBe 0

    kim.exMachina("6 MC")
    kim.convertPlants {
          doTask("GreeneryTile<Hellas_9_7>")
          doTask("OceanTile<Hellas_6_7>")
        }
        .expect("GreeneryTile<Hellas_9_7>, -6 MC, OceanTile<Hellas_6_7>")
  }

  @Test
  internal fun `Greenery can still be placed after oxygen is maximized`() {
    newTestGame()
    kim.exMachina("8 Plant, 13 OxygenStep")
    stan.stdProject("GreeneryProject") { placeTile(1, 1) }

    kim.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, 0 OxygenStep, 0 TerraformRating")
  }

  @Test
  internal fun `Reaching eight percent oxygen also raises temperature`() {
    newTestGame()
    kim.exMachina("8 Plant, 7 OxygenStep")

    kim.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, OxygenStep, TemperatureStep, 2 TerraformRating")
  }

  @Test
  internal fun `Tile placement grants both area and ocean adjacency bonuses`() {
    newTestGame()
    kim.exMachina("8 Plant, OceanTile<Tharsis_4_8>")

    kim.convertPlants { doTask("GreeneryTile<Tharsis_4_7>") }
        .expect("-7 Plant, 2 MC, GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Standard projects perform their advertised effects`() {
    newTestGame()
    kim.setToExMachina(100, "MC")

    kim.stdProject("PowerPlantProject").expect("PROD[Energy]")
    kim.stdProject("AsteroidProject").expect("TemperatureStep, TerraformRating")
    kim.stdProject("AquiferProject") { doTask("OceanTile<Tharsis_1_2>") }
        .expect("OceanTile, TerraformRating")
    kim.stdProject("CityProject") { doTask("CityTile<Tharsis_4_4>") }.expect("CityTile, PROD[1 MC]")
    kim.stdProject("GreeneryProject") { doTask("GreeneryTile<Tharsis_4_5>") }
        .expect("GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `A qualified player can claim a milestone`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")

    kim.claimMilestone(cn("Terraformer35")).expect("-8 MC, Milestone")
  }

  @Test
  internal fun `A milestone cannot be claimed twice`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")
    stan.setToExMachina(35, "TerraformRating")
    kim.claimMilestone(cn("Terraformer35"))

    shouldThrow<LimitsException> { stan.claimMilestone(cn("Terraformer35")) }
  }

  @Test
  internal fun `Funding successive awards costs eight fourteen and twenty`() {
    newTestGame()

    kim.fundAward(cn("Landlord"), 8).expect("-8 MC, Award")
    kim.fundAward(cn("Scientist"), 14).expect("-14 MC, Award")
    kim.fundAward(cn("Thermalist"), 20).expect("-20 MC, Award")
  }

  @Test
  internal fun `A colony trade can be paid for with three energy`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.exMachina("3 Energy")

    kim.stdAction("TradeAction", 2) { doTask("Trade<Ceres>") }.expect("-3 Energy, 2 Steel")
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
