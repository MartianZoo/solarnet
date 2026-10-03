package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.Tr63SoloObjective
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EndgameRulesTest : CardTest() {
  @Test
  internal fun `Final production occurs before players place their final greeneries`() {
    newGame()
    p1.runOperation("PROD[Steel], 8 Plant")
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    val workflow = TfmWorkflow.Stepwise(agents)

    workflow.productionPhase()
    workflow.solarPhase() shouldBe null
    workflow.finalGreeneryPhase()
    p1.startTurn()
    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_3>") }

    p1.count("Steel") shouldBe 1
    p1.count("GreeneryTile") shouldBe 1
  }

  @Test
  internal fun `Standard solo victory requires completing all base global parameters`() {
    newGame(players = 1)
    exhaustSoloCountdown()
    admin.runOperation("CheckGameEnd")
    p1.count("Victory") shouldBe 0

    newGame(players = 1)
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    exhaustSoloCountdown()
    admin.runOperation("CheckGameEnd")

    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `Standard Venus solo also requires completing Venus`() {
    newGame(VenusNextExpansion, players = 1)
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    admin.runOperation("CheckGameEnd")
    p1.count("Victory") shouldBe 0

    newGame(VenusNextExpansion, players = 1)
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, GpComplete<Class<VenusStep>>"
    )
    admin.runOperation("CheckGameEnd")

    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `Mandatory Venus variant delays multiplayer end until Venus is complete`() {
    newGame(GameConfig("VenusNextExpansion, MandatoryVenusVariant", "Player1", "Player2"))
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>"
    )
    admin.count("GameEndBarrier") shouldBe 1
    (TfmWorkflow.Stepwise(agents).solarPhase() == null) shouldBe false

    newGame(GameConfig("VenusNextExpansion, MandatoryVenusVariant", "Player1", "Player2"))
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, GpComplete<Class<VenusStep>>"
    )
    admin.count("GameEndBarrier") shouldBe 0
    TfmWorkflow.Stepwise(agents).solarPhase() shouldBe null
  }

  @Test
  internal fun `Prelude shortens either solo objective by two generations`() {
    newGame(players = 1)
    admin.count("SoloGenerationsLeft") shouldBe 13

    newGame(PreludeExpansion, players = 1)
    admin.count("SoloGenerationsLeft") shouldBe 11

    newGame(PreludeExpansion, Tr63SoloObjective, players = 1)
    admin.count("SoloGenerationsLeft") shouldBe 11
    p1.count("Tr63SoloObjective") shouldBe 1
    p1.count("StandardSoloObjective") shouldBe 0
  }

  @Test
  internal fun `TR 63 solo ignores completed parameters below 63 and wins at 63`() {
    newGame(VenusNextExpansion, Tr63SoloObjective, players = 1)
    p1.runOperation("48 TerraformRating")
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, GpComplete<Class<VenusStep>>, CheckGameEnd"
    )
    p1.count("Victory") shouldBe 0

    p1.runOperation("TerraformRating")
    admin.runOperation("CheckGameEnd")

    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `TR 63 final greenery scores without raising oxygen or rating`() {
    // Jacob corrected his earlier TR 63 ruling: https://boardgamegeek.com/article/37132579
    newGame(Tr63SoloObjective, players = 1)
    p1.runOperation("49 TerraformRating, 8 Plant")
    exhaustSoloCountdown()
    admin.runOperation("CheckGameEnd")
    p1.count("Victory") shouldBe 1
    p1.count("TerraformRating") shouldBe 63

    TfmWorkflow.Stepwise(agents).finalGreeneryPhase()
    p1.startTurn()
    p1.convertPlants { placeTile(3, 3) }
    admin.runOperation("End FROM Phase")

    admin.count("OxygenStep") shouldBe 0
    p1.count("TerraformRating") shouldBe 63
    p1.count("VictoryPoint") shouldBe 64
    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `TR 63 solo evaluates the current rating rather than past attainment`() {
    newGame(Tr63SoloObjective, players = 1)
    p1.runOperation("49 TerraformRating")
    p1.runOperation("-TerraformRating")

    admin.runOperation("CheckGameEnd")

    p1.count("Victory") shouldBe 0
  }

  private fun exhaustSoloCountdown() {
    repeat(admin.count("SoloGenerationsLeft")) {
      admin.runOperation("-SoloGenerationsLeft")
    }
  }
}
