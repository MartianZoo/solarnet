package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TfmWorkflow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CoreRulesTest : CardTest() {
  @Test
  internal fun `Research cards cost three megacredits each`() {
    newGame()
    p1.manual("12 Megacredit")

    engine.phase("Research") {
      p1.buyCards(3)
      requireP2().buyCards(0)
    }

    p1.count("Megacredit") shouldBe 3
    p1.count("ProjectCard") shouldBe 3
  }

  @Test
  internal fun `Selling patents returns one megacredit per card`() {
    newGame()
    p1.manual("3 ProjectCard")
    engine.phase("Action")

    p1.sellPatents(2).expect("-2 ProjectCard, 2 Megacredit")
  }

  @Test
  internal fun `Eight heat raises temperature and terraform rating`() {
    newGame()
    p1.manual("8 Heat")
    engine.phase("Action")

    p1.convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating")
  }

  @Test
  internal fun `Temperature track bonuses are resolved during heat conversion`() {
    newGame()
    p1.manual("8 Heat, 2 TemperatureStep")
    engine.phase("Action")

    p1.convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, PROD[Heat]")
  }

  @Test
  internal fun `Reaching zero degrees also places an ocean`() {
    newGame()
    p1.manual("8 Heat, 14 TemperatureStep")
    engine.phase("Action")

    p1.convertHeat { doTask("OceanTile<Tharsis_1_2>") }
        .expect("-8 Heat, TemperatureStep, OceanTile, 2 TerraformRating")
  }

  @Test
  internal fun `Eight plants place greenery and raise oxygen and terraform rating`() {
    newGame()
    p1.manual("8 Plant")
    engine.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Greenery can still be placed after oxygen is maximized`() {
    newGame()
    p1.manual("8 Plant, 14 OxygenStep")
    engine.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, 0 OxygenStep, 0 TerraformRating")
  }

  @Test
  internal fun `Reaching eight percent oxygen also raises temperature`() {
    newGame()
    p1.manual("8 Plant, 7 OxygenStep")
    engine.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }
        .expect("-8 Plant, GreeneryTile, OxygenStep, TemperatureStep, 2 TerraformRating")
  }

  @Test
  internal fun `Tile placement grants both area and ocean adjacency bonuses`() {
    newGame()
    p1.manual("8 Plant, OceanTile<Tharsis_4_8>")
    engine.phase("Action")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_4_7>") }
        .expect("-7 Plant, 2 Megacredit, GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Standard projects perform their advertised effects`() {
    newGame()
    p1.manual("100 Megacredit")
    engine.phase("Action")

    p1.stdProject("PowerPlantSP").expect("PROD[Energy]")
    p1.stdProject("AsteroidSP").expect("TemperatureStep, TerraformRating")
    p1.stdProject("AquiferSP") { doTask("OceanTile<Tharsis_1_2>") }
        .expect("OceanTile, TerraformRating")
    p1.stdProject("CitySP") { doTask("CityTile<Tharsis_4_4>") }.expect("CityTile, PROD[Megacredit]")
    p1.stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_4_5>") }
        .expect("GreeneryTile, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `A qualified player can claim a milestone`() {
    newGame()
    p1.manual("8 Megacredit, 15 TerraformRating")
    engine.phase("Action")

    p1.stdAction("ClaimMilestoneSA") { doTask("Terraformer35") }.expect("-8 Megacredit, Milestone")
  }

  @Test
  internal fun `Funding successive awards costs eight fourteen and twenty`() {
    newGame()
    p1.manual("42 Megacredit")
    engine.phase("Action")

    p1.stdAction("FundAwardSA") { doTask("Landlord") }.expect("-8 Megacredit, Award")
    p1.stdAction("FundAwardSA", which = 2) { doTask("Scientist") }.expect("-14 Megacredit, Award")
    p1.stdAction("FundAwardSA", which = 3) { doTask("Thermalist") }.expect("-20 Megacredit, Award")
  }

  @Test
  internal fun `Production converts existing energy before producing new resources`() {
    newGame()
    p1.manual("2 Energy, PROD[Energy], PROD[2 Steel]")

    engine.phase("Production")

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
    engine.phase("Action")

    p1.stdAction("TradeSA", 2) { doTask("Trade<Ceres>") }.expect("-3 Energy, 2 Steel")
  }

  @Test
  internal fun `World Government terraforming gives no terraform rating`() {
    newGame(VenusNextExpansion)

    TfmWorkflow.Manual(game).solarPhase()

    p1.doTask("TemperatureStep! BY Engine").expect("TemperatureStep, 0 TerraformRating")
  }
}
