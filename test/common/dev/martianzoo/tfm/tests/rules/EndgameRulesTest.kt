package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.Tr63SoloVariant
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EndgameRulesTest : CardTest() {
  @Test
  internal fun `Final production occurs before players place their final greeneries`() {
    newGame()
    p1.manual("PROD[Steel], 8 Plant")
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    val workflow = TfmWorkflow.Manual(game)

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
    engine.manual("CheckGameEnd")
    p1.count("Victory") shouldBe 0

    newGame(players = 1)
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    exhaustSoloCountdown()
    engine.manual("CheckGameEnd")

    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `Standard Venus solo also requires completing Venus`() {
    newGame(VenusNextExpansion, players = 1)
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    engine.manual("CheckGameEnd")
    p1.count("Victory") shouldBe 0

    newGame(VenusNextExpansion, players = 1)
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, GpComplete<Class<VenusStep>>"
    )
    engine.manual("CheckGameEnd")

    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `Prelude shortens the solo countdown by two generations`() {
    newGame(players = 1)
    engine.count("SoloGenerationsLeft") shouldBe 13

    newGame(PreludeExpansion, players = 1)
    engine.count("SoloGenerationsLeft") shouldBe 11
  }

  @Test
  internal fun `TR 63 solo ignores completed parameters below 63 and wins at 63`() {
    newGame(VenusNextExpansion, Tr63SoloVariant, players = 1)
    p1.manual("48 TerraformRating")
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, GpComplete<Class<VenusStep>>, CheckGameEnd"
    )
    p1.count("Victory") shouldBe 0

    p1.manual("TerraformRating")
    engine.manual("CheckGameEnd")

    p1.count("Victory") shouldBe 1
  }

  @Test
  internal fun `TR 63 solo evaluates the current rating rather than past attainment`() {
    newGame(Tr63SoloVariant, players = 1)
    p1.manual("49 TerraformRating")
    p1.manual("-TerraformRating")

    engine.manual("CheckGameEnd")

    p1.count("Victory") shouldBe 0
  }

  private fun exhaustSoloCountdown() {
    repeat(engine.count("SoloGenerationsLeft")) {
      engine.manual("-SoloGenerationsLeft")
    }
  }
}
