package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EndgameRulesTest : TfmGameplayTest() {
  @Test
  internal fun `Final production supplies plants before final greenery without further terraforming`() {
    reachMultiplayerEnd()
    stan.pass()
    val plants = kim.count("Plant")
    val steel = kim.count("Steel")
    kim.pass()
    kim.count("Plant") shouldBe plants + 1
    kim.count("Steel") shouldBe steel + 1
    stan.doTask("Ok") // Decline Stan's final greenery.
    kim.convertPlants { placeTile(3, 3) }
        .expect("-8 Plant, GreeneryTile, 0 OxygenStep, 0 TerraformRating")
    kim.doTask("Ok") // Decline another final greenery.
    kim.count("VictoryPoint") shouldBe 21
    stan.count("VictoryPoint") shouldBe 20
  }

  @Test
  internal fun `MC breaks an otherwise tied multiplayer final score`() {
    reachMultiplayerEnd()
    stan.pass()
    kim.sellPatents(1).expect("MC, -ProjectCard")
    kim.pass()
    stan.doTask("Ok") // No final greenery.
    kim.doTask("Ok") // No final greenery.
    kim.count("VictoryPoint") shouldBe 20
    stan.count("VictoryPoint") shouldBe 20
    kim.count("MC") shouldBe stan.count("MC") + 1
    kim.count("Victory") shouldBe 1
    stan.count("Victory") shouldBe 0
  }

  @Test
  internal fun `Mandatory Venus keeps multiplayer going after the base parameters are complete`() {
    reachMultiplayerEnd("MandatoryVenusVariant")
    // Generation 42 still has a Solar phase because Venus is unfinished.
    repeat(15) { finishMultiplayerGeneration("VenusStep") }
    admin.count("Generation") shouldBe 57
    kim.pass()
    stan.pass()
    kim.doTask("Ok") // No final greenery.
    stan.doTask("Ok") // No final greenery.
    admin.count("End") shouldBe 1
  }

  @Test
  internal fun `An unfinished standard solo game loses after fourteen generations`() {
    newTestGame(addOptions = "-VenusNextExpansion", playerCount = 1)
    repeat(13) {
      kim.pass()
      kim.buyCards(0)
    }
    kim.pass()
    admin.count("Generation") shouldBe 14
    kim.count("Victory") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 0
  }

  @Test
  internal fun `Prelude shortens the standard solo game to twelve generations`() {
    newTestGame(addOptions = "PreludeExpansion, -VenusNextExpansion", playerCount = 1)
    kim.turn {
      playPrelude(Donation)
      playPrelude(AlliedBank)
    }
    repeat(11) {
      kim.pass()
      kim.buyCards(0)
    }
    kim.pass()
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
  }

  @Test
  internal fun `Prelude also shortens the TR63 game to twelve generations`() {
    newTestGame(
        addOptions = "PreludeExpansion, Tr63SoloObjective, -VenusNextExpansion",
        playerCount = 1,
    )
    kim.turn {
      playPrelude(Donation)
      playPrelude(AlliedBank)
    }
    repeat(11) {
      kim.pass()
      kim.buyCards(0)
    }
    kim.pass()
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
  }

  @Test
  internal fun `Completing the base parameters wins standard solo without Venus`() {
    prepareSoloEconomy("-VenusNextExpansion")
    playSoloTerraforming(completeVenus = false)
    kim.count("Victory") shouldBe 1
    kim.doTask("Ok") // Decline final greenery.
    admin.count("End") shouldBe 1
  }

  @Test
  internal fun `Completing only the base parameters loses standard solo with Venus`() {
    prepareSoloEconomy("")
    playSoloTerraforming(completeVenus = false)
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
    admin.count("TemperatureStep") shouldBe 19
    admin.count("OxygenStep") shouldBe 14
    admin.count("OceanTile") shouldBe 9
    admin.count("VenusStep") shouldBe 0
  }

  @Test
  internal fun `Completing Venus as well wins standard solo`() {
    prepareSoloEconomy("")
    playSoloTerraforming(completeVenus = true)
    kim.count("Victory") shouldBe 1
    kim.doTask("Ok") // Decline final greenery.
  }

  @Test
  internal fun `TR63 wins with incomplete parameters and final greenery scores without TR`() {
    // Jacob corrected his earlier TR63 ruling: https://boardgamegeek.com/article/37132579
    prepareSoloEconomy("Tr63SoloObjective, -VenusNextExpansion")
    playSoloBufferGas(63)
    kim.count("Victory") shouldBe 1
    admin.count("OxygenStep") shouldBe 0
    kim.convertPlants { placeTile(3, 3) }
        .expect("GreeneryTile, -8 Plant, 0 OxygenStep, 0 TerraformRating")
    kim.doTask("Ok") // Decline further final greeneries.
    kim.count("TerraformRating") shouldBe 63
    // Miranda Resort supplies one printed VP in addition to the final greenery.
    kim.count("VictoryPoint") shouldBe 65
  }

  @Test
  internal fun `TR63 loses at sixty two despite abundant money and plants`() {
    prepareSoloEconomy("Tr63SoloObjective, -VenusNextExpansion")
    playSoloBufferGas(62)
    admin.count("Generation") shouldBe 12
    kim.count("TerraformRating") shouldBe 62
    kim.count("Victory") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 0
  }

  @Test
  internal fun `Completing Mars below sixty three TR does not win the TR63 objective`() {
    prepareSoloEconomy("Tr63SoloObjective, -VenusNextExpansion")
    playSoloTerraforming(completeVenus = false)
    admin.count("Generation") shouldBe 12
    admin.count("TemperatureStep") shouldBe 19
    admin.count("OxygenStep") shouldBe 14
    admin.count("OceanTile") shouldBe 9
    kim.count("TerraformRating") shouldBe 56
    kim.count("Victory") shouldBe 0
  }

  @Test
  internal fun `Losing rating after reaching sixty three still loses the solo game`() {
    prepareSoloEconomy(
        "Tr63SoloObjective, -VenusNextExpansion, BoardOfDirectors, Pristar",
        BoardOfDirectors,
    )
    repeat(11) {
      while (kim.count("MC") >= 16 && kim.count("TerraformRating") < 63) kim.stdProject(
          "BufferGasProject"
      )
      kim.pass()
      kim.buyCards(0)
    }
    while (kim.count("TerraformRating") < 63) kim.stdProject("BufferGasProject")
    kim.count("TerraformRating") shouldBe 63
    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(Merger) { kim.playCorp(Pristar) }
        }
        .expect("-2 TerraformRating")
    kim.pass()
    admin.count("Generation") shouldBe 12
    kim.count("TerraformRating") shouldBe 61
    kim.count("Victory") shouldBe 0
  }

  private fun reachMultiplayerEnd(options: String = "") {
    newTestGame(addOptions = options, playerCount = 2)
    // World Government completes Mars without giving either player TR or owned tiles.
    repeat(19) { finishMultiplayerGeneration("TemperatureStep") }
    repeat(8) { finishMultiplayerGeneration("OceanTile") }
    repeat(14) { finishMultiplayerGeneration("OxygenStep") }
    admin.count("Generation") shouldBe 42
  }

  private fun finishMultiplayerGeneration(parameter: String) {
    val first = players.single { it.count("StartToken") == 1 }
    val other = players.single { it != first }
    first.pass()
    other.pass()
    val temperature = admin.count("TemperatureStep")
    if (parameter == "OceanTile") {
      first.wgt("OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>")
    } else {
      first.wgt(parameter)
      if (parameter == "TemperatureStep" && temperature == 14) {
        first.doTask("OceanTile<Tharsis_1_2> BY Admin")
      }
    }
    players.forEach { it.buyCards(0) }
  }

  private fun prepareSoloEconomy(options: String, secondPrelude: ClassName = AlliedBank) {
    newTestGame(addOptions = "PreludeExpansion, -WorldGovernmentRule, $options", playerCount = 1)
    kim.turn {
      playPrelude(BusinessEmpire)
      playPrelude(secondPrelude)
    }
    // Ordinary early investments fund the later standard projects within twelve generations.
    kim.playProject(EarthOffice, 1)
    kim.playProject(LunaGovernor, 0)
    kim.playProject(Sponsors, 3)
    kim.playProject(AcquiredCompany, 7)
    kim.playProject(Cartel, 5)
    kim.playProject(MirandaResort, 12)
    kim.playProject(StandardTechnology, 6)
  }

  private fun playSoloBufferGas(target: Int) {
    repeat(12) { generation ->
      while (kim.count("MC") >= 16 && kim.count("TerraformRating") < target) {
        kim.stdProject("BufferGasProject")
      }
      kim.pass()
      if (generation < 11) kim.buyCards(0)
    }
  }

  private fun playSoloTerraforming(completeVenus: Boolean) {
    repeat(12) { generation ->
      while (kim.count("MC") >= 14 && admin.count("TemperatureStep") < 19) {
        val temperature = admin.count("TemperatureStep")
        kim.stdProject("AsteroidProject") {
          if (temperature == 14)
              doTask("OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>")
        }
      }
      while (kim.count("MC") >= 18 && admin.count("OceanTile") < 9) {
        kim.stdProject("AquiferProject") {
          doTask("OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>")
        }
      }
      while (kim.count("MC") >= 23 && admin.count("OxygenStep") < 14) {
        val adjacent = kim.list("LandArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile>)")
        val area = (adjacent.ifEmpty { kim.list("LandArea(HAS MAX 0 Tile)") }).first()
        kim.stdProject("GreeneryProject") { doTask("GreeneryTile<$area>") }
      }
      if (completeVenus) {
        while (kim.count("MC") >= 15 && admin.count("VenusStep") < 15) {
          kim.stdProject("AirScrappingProject")
        }
      }
      kim.pass()
      if (generation < 11) kim.buyCards(0)
    }
  }
}
