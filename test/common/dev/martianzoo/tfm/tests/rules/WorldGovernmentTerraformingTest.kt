package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.engine.*
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    newTestGame()
    // Arrange for this generation to end; earlier track completion is irrelevant to phase order.
    // Leave the tracks themselves low so an erroneous Solar Phase would still offer terraforming.
    admin.exMachina(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    rob.doTask("Pass")
    players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }

    admin.count("SolarPhase") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 1
  }

  internal class Attribution {
    @Test
    internal fun `first player chooses the threshold ocean after Admin raises temperature`() {
      val game = setUpGame(VenusNextExpansion, players = 2)
      val admin = game.testAgent(ADMIN)
      val p2 = game.testAgent(PLAYER2)
      admin.runOperation("StartToken<Player2> FROM StartToken<Player1>")
      admin.runOperation("14 TemperatureStep")
      val checkpoint = game.events.nextOrdinal

      admin.runOperation("TemperatureStep") { p2.doTask("OceanTile<Tharsis_1_2> BY Admin") }

      val ocean =
          game.events.changesSince(checkpoint).single {
            it.change.gaining?.className.toString() == "OceanTile"
          }
      ocean.actor shouldBe ADMIN
      admin.count("TemperatureStep") shouldBe 15
    }

    @Test
    internal fun `World Government is skipped after every parameter is complete`() {
      val game = setUpGame(VenusNextExpansion)
      val admin = game.testAgent(ADMIN)
      admin.runOperation(
          "GpComplete<Class<TemperatureStep>>, " +
              "GpComplete<Class<OxygenStep>>, " +
              "GpComplete<Class<OceanTile>>, " +
              "GpComplete<Class<VenusStep>>"
      )
      admin.count("GpIncomplete") shouldBe 0

      admin.runOperation("WorldGovernmentTerraforming<Player1>")

      game.tasks.ids() shouldBe emptySet()
    }
  }
}
