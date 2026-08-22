package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.Tr63SoloVariant
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TfmWorkflow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EndgameRulesTest : CardTest() {
  @Test
  fun `Final production occurs before players place their final greeneries`() {
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
  fun `Standard solo victory requires completing all base global parameters`() {
    newGame(players = 1)
    exhaustSoloCountdown()
    engine.manual("SoloVictoryCheck")
    p1.count("Victory") shouldBe 0

    newGame(players = 1)
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )
    exhaustSoloCountdown()
    engine.manual("SoloVictoryCheck")

    p1.count("Victory") shouldBe 1
  }

  @Test
  fun `Prelude shortens the solo countdown by two generations`() {
    newGame(players = 1)
    engine.count("SoloGenerationsLeft") shouldBe 13

    newGame(PreludeExpansion, players = 1)
    engine.count("SoloGenerationsLeft") shouldBe 11
  }

  @Test
  fun `TR 63 solo ignores completed parameters below 63 and wins at 63`() {
    newGame(VenusNextExpansion, Tr63SoloVariant, players = 1)
    p1.manual("48 TerraformRating")
    engine.manual(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, GpComplete<Class<VenusStep>>, SoloVictoryCheck"
    )
    p1.count("Victory") shouldBe 0

    p1.manual("TerraformRating")
    engine.manual("SoloVictoryCheck")

    p1.count("Victory") shouldBe 1
  }

  private fun exhaustSoloCountdown() {
    repeat(engine.count("SoloGenerationsLeft")) { engine.manual("-SoloGenerationsLeft") }
  }
}
