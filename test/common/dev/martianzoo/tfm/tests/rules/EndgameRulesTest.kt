package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EndgameRulesTest : TfmGameplayTest() {
  @Test
  internal fun `Final production supplies plants before final greenery without further terraforming`() {
    reachMultiplayerEnd()
    stan.doTask("Pass")
    expectChanges("1 Plant<Kim>, 1 Steel<Kim>") {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
    }
    admin.count("ActionScope") shouldBe 0
    kim.count("Pass") shouldBe 1
    stan.selectTask("PassBaton") // Decline Stan's final greenery.
    admin.count("ActionScope") shouldBe 0
    kim.completeOperation {
          doTasks("ConvertPlants")
          admin.count("ActionScope<Kim>") shouldBe 1
          doTask("GreeneryTile<Tharsis_3_3>")
        }
        .expect("-8 Plant, GreeneryTile, 0 OxygenStep, 0 TerraformRating")
    admin.count("ActionScope") shouldBe 0
    kim.count("Pass") shouldBe 1
    kim.selectTask("PassBaton") // Decline another final greenery.
    admin.count("ActionScope") shouldBe 0
    kim.count("VictoryPoint") shouldBe 21
    stan.count("VictoryPoint") shouldBe 20
  }

  @Test
  internal fun `MC breaks an otherwise tied multiplayer final score`() {
    reachMultiplayerEnd()
    stan.doTask("Pass")
    kim.doTasks("UseStandardProject<SellPatentsProject>", "1 MC FROM ProjectCard")
        .expect("MC, -ProjectCard")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.selectTask("PassBaton") // No final greenery.
    kim.selectTask("PassBaton") // No final greenery.
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
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.selectTask("PassBaton") // No final greenery.
    stan.selectTask("PassBaton") // No final greenery.
    admin.count("End") shouldBe 1
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `An unfinished standard solo game loses after fourteen generations`() {
    newTestGame(addOptions = "-VenusNextExpansion", playerCount = 1)
    repeat(13) {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("-4 ProjectCard<Selecting>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("Generation") shouldBe 14
    kim.count("Victory") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Prelude shortens the standard solo game to twelve generations`() {
    newTestGame(addOptions = "PreludeExpansion, -VenusNextExpansion", playerCount = 1)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$AlliedBank>>")
    kim.autoExecNow(EAGER)

    repeat(11) {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("-4 ProjectCard<Selecting>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Prelude also shortens the TR63 game to twelve generations`() {
    newTestGame(
        addOptions = "PreludeExpansion, Tr63SoloObjective, -VenusNextExpansion",
        playerCount = 1,
    )
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$AlliedBank>>")
    kim.autoExecNow(EAGER)

    repeat(11) {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("-4 ProjectCard<Selecting>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Completing the base parameters wins standard solo without Venus`() {
    prepareSoloEconomy("-VenusNextExpansion")
    playSoloTerraforming(completeVenus = false)
    kim.count("Victory") shouldBe 1
    admin.count("GameScope") shouldBe 1
    kim.selectTask("PassBaton") // Decline final greenery.
    admin.count("End") shouldBe 1
    admin.count("GameScope") shouldBe 0
    game.events.changesSinceSetup().last().change.removing?.type?.className shouldBe cn("GameScope")
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
    kim.selectTask("PassBaton") // Decline final greenery.
  }

  @Test
  internal fun `TR63 wins with incomplete parameters and final greenery scores without TR`() {
    // Jacob corrected his earlier TR63 ruling: https://boardgamegeek.com/article/37132579
    prepareSoloEconomy("Tr63SoloObjective, -VenusNextExpansion")
    playSoloBufferGas(63)
    kim.count("Victory") shouldBe 1
    admin.count("OxygenStep") shouldBe 0
    kim.completeOperation {
          doTasks("ConvertPlants")
          doTask("GreeneryTile<Tharsis_3_3>")
        }
        .expect("GreeneryTile, -8 Plant, 0 OxygenStep, 0 TerraformRating")
    kim.selectTask("PassBaton") // Decline further final greeneries.
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
      while (kim.count("MC") >= 16 && kim.count("TerraformRating") < 63) kim.completeOperation {
        doTasks(
            "UseStandardProject<BufferGasProject>",
        )
        autoExecNow(EAGER)
      }
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("-4 ProjectCard<Selecting>")
    }
    while (kim.count("TerraformRating") < 63) kim.completeOperation {
      doTasks(
          "UseStandardProject<BufferGasProject>",
      )
      autoExecNow(EAGER)
    }
    kim.count("TerraformRating") shouldBe 63
    kim.completeOperation {
          doTask("UseCardAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          autoExecNow(EAGER)
          doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
          doTask("PlayCard<Class<CorporationCard>, Class<$Pristar>>")
          autoExecNow(EAGER)
        }
        .expect("-2 TerraformRating")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
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
    first.doTask("Pass")
    other.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    val temperature = admin.count("TemperatureStep")
    if (parameter == "OceanTile") {
      first.doTask("OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>! BY Admin")
    } else {
      first.doTask("$parameter! BY Admin")

      if (parameter == "TemperatureStep" && temperature == 14) {
        first.doTask("OceanTile<Tharsis_1_2> BY Admin")
      }
    }
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  private fun prepareSoloEconomy(options: String, secondPrelude: ClassName = AlliedBank) {
    val worldGovernment = if ("-VenusNextExpansion" in options) "" else "-WorldGovernmentRule, "
    newTestGame(addOptions = "PreludeExpansion, $worldGovernment$options", playerCount = 1)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$BusinessEmpire>>")
    kim.autoExecNow(EAGER)

    kim.doTask("PlayCard<Class<PreludeCard>, Class<$secondPrelude>>")
    kim.autoExecNow(EAGER)
    // Ordinary early investments fund the later standard projects within twelve generations.
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    kim.doTask("PlayProject<Class<$LunaGovernor>>")
    kim.doTasks("PlayProject<Class<$Sponsors>>")
    kim.doTasks("PlayProject<Class<$AcquiredCompany>>")
    kim.doTasks("PlayProject<Class<$Cartel>>")
    kim.doTasks("PlayProject<Class<$MirandaResort>>")
    kim.doTasks("PlayProject<Class<$StandardTechnology>>")
  }

  private fun playSoloBufferGas(target: Int) {
    repeat(12) { generation ->
      while (kim.count("MC") >= 16 && kim.count("TerraformRating") < target) {
        kim.completeOperation {
          doTasks("UseStandardProject<BufferGasProject>")
          autoExecNow(EAGER)
        }
      }
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      if (generation < 11) kim.doTask("-4 ProjectCard<Selecting>")
    }
  }

  private fun playSoloTerraforming(completeVenus: Boolean) {
    repeat(12) { generation ->
      while (kim.count("MC") >= 14 && admin.count("TemperatureStep") < 19) {
        val temperature = admin.count("TemperatureStep")
        kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)

          if (temperature == 14)
              doTask("OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>")
          autoExecNow(EAGER)
        }
      }
      while (kim.count("MC") >= 18 && admin.count("OceanTile") < 9) {
        kim.completeOperation {
          doTasks("UseStandardProject<AquiferProject>")
          doTask("OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>")
          autoExecNow(EAGER)
        }
      }
      while (kim.count("MC") >= 23 && admin.count("OxygenStep") < 14) {
        val adjacent = kim.list("LandArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile>)")
        val area = (adjacent.ifEmpty { kim.list("LandArea(HAS MAX 0 Tile)") }).first()
        kim.completeOperation {
          doTasks("UseStandardProject<GreeneryProject>")
          doTask("GreeneryTile<$area>")
          autoExecNow(EAGER)
        }
      }
      if (completeVenus) {
        while (kim.count("MC") >= 15 && admin.count("VenusStep") < 15) {
          kim.completeOperation {
            doTasks("UseStandardProject<AirScrappingProject>")
            autoExecNow(EAGER)
          }
        }
      }
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      if (generation < 11) kim.doTask("-4 ProjectCard<Selecting>")
    }
  }
}
