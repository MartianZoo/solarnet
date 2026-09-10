package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecMode
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CoreRulesTest : CardTest() {
  @Test
  internal fun `Research cards cost three mc each`() {
    newGame()
    p1.manual("12 MC")
    p1.autoExecMode = AutoExecMode.SAFE
    requireP2().autoExecMode = AutoExecMode.SAFE
    admin.count("Generation") shouldBe 1

    admin.phase("Research") {
      p1.buyCards(3)
      requireP2().buyCards(0)
    }

    p1.count("MC") shouldBe 3
    p1.count("ProjectCard") shouldBe 3
    admin.count("Generation") shouldBe 2
  }

  @Test
  internal fun `Selling patents returns one mc per card`() {
    newGame()
    p1.manual("3 ProjectCard")
    admin.phase("Action")

    p1.sellPatents(2).expect("-2 ProjectCard, 2 MC")
  }

  @Test
  internal fun `Eight heat raises temperature and terraform rating`() {
    newGame()
    p1.manual("8 Heat")
    admin.phase("Action")

    p1.convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating")
  }

  @Test
  internal fun `Temperature track bonuses are resolved during heat conversion`() {
    newGame()
    p1.manual("8 Heat, 2 TemperatureStep")
    admin.phase("Action")

    p1.convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, PROD[Heat]")
  }

  @Test
  internal fun `Reaching zero degrees also places an ocean`() {
    newGame()
    p1.manual("8 Heat, 14 TemperatureStep")
    admin.phase("Action")

    p1.convertHeat { doTask("OceanTile<Tharsis_1_2>") }
        .expect("-8 Heat, TemperatureStep, OceanTile, 2 TerraformRating")
  }

  @Test
  internal fun `Eight plants place greenery and raise oxygen and terraform rating`() {
    newGame()
    p1.manual("8 Plant")
    admin.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Greenery can still be placed after oxygen is maximized`() {
    newGame()
    p1.manual("8 Plant, 14 OxygenStep")
    admin.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, 0 OxygenStep, 0 TerraformRating")
  }

  @Test
  internal fun `Reaching eight percent oxygen also raises temperature`() {
    newGame()
    p1.manual("8 Plant, 7 OxygenStep")
    admin.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, OxygenStep, TemperatureStep, 2 TerraformRating")
  }

  @Test
  internal fun `Tile placement grants both area and ocean adjacency bonuses`() {
    newGame()
    p1.manual("8 Plant, OceanTile<Tharsis_4_8>")
    admin.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_4_7>") }
        .expect("-7 Plant, 2 MC, GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Standard projects perform their advertised effects`() {
    newGame()
    p1.manual("100 MC")
    admin.phase("Action")

    p1.stdProject("PowerPlantProject").expect("PROD[Energy]")
    p1.stdProject("AsteroidProject").expect("TemperatureStep, TerraformRating")
    p1.stdProject("AquiferProject") { doTask("OceanTile<Tharsis_1_2>") }
        .expect("OceanTile, TerraformRating")
    p1.stdProject("CityProject") { doTask("CityTile<Tharsis_4_4>") }.expect("CityTile, PROD[1 MC]")
    p1.stdProject("GreeneryProject") { doTask("GreeneryTile<Tharsis_4_5>") }
        .expect("GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `A qualified player can claim a milestone`() {
    newGame()
    p1.manual("8 MC, 15 TerraformRating")
    admin.phase("Action")

    p1.claimMilestone(cn("Terraformer35")).expect("-8 MC, Milestone")
  }

  @Test
  internal fun `A milestone cannot be claimed twice`() {
    newGame()
    p1.manual("35 TerraformRating")
    requireP2().manual("35 TerraformRating")
    p1.manual("Terraformer35")

    shouldThrow<LimitsException> { requireP2().manual("Terraformer35") }
  }

  @Test
  internal fun `Funding successive awards costs eight fourteen and twenty`() {
    newGame()
    p1.manual("42 MC")
    admin.phase("Action")

    p1.fundAward(cn("Landlord"), 8).expect("-8 MC, Award")
    p1.fundAward(cn("Scientist"), 14).expect("-14 MC, Award")
    p1.fundAward(cn("Thermalist"), 20).expect("-20 MC, Award")
  }

  @Test
  internal fun `Production converts existing energy before producing new resources`() {
    newGame()
    p1.manual("2 Energy, PROD[Energy], PROD[2 Steel]")

    admin.phase("Production")

    p1.count("Energy") shouldBe 1
    p1.count("Heat") shouldBe 2
    p1.count("Steel") shouldBe 2
  }

  @Test
  internal fun `A colony trade can be paid for with three energy`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(players = 2, "Ceres"),
    )
    p1.manual("3 Energy")
    admin.phase("Action")

    p1.stdAction("TradeAction", 2) { doTask("Trade<Ceres>") }.expect("-3 Energy, 2 Steel")
  }

  @Test
  internal fun `World Government terraforming gives no terraform rating`() {
    newGame(VenusNextExpansion)

    TfmWorkflow.Manual(game).solarPhase()

    p1.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 TerraformRating")
  }
}
