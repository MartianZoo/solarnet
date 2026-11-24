package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.engine.*
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest : TfmGameplayTest() {
  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    newTestGame()
    // Arrange for this generation to end; earlier track completion is irrelevant to phase order.
    // Leave the tracks themselves low so an erroneous Solar Phase would still offer terraforming.
    agents.correct(
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
      val agents = Agents(game) { NONE }
      val admin = agents.world.reader
      val p2 = agents[PLAYER2]
      agents.runSystem("StartToken<Player2> FROM StartToken<Player1>")
      agents.runSystem("14 TemperatureStep")
      val checkpoint = game.events.nextOrdinal

      agents.runSystem("TemperatureStep") { p2.doTask("OceanTile<Tharsis_1_2> BY Admin") }

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
      val agents = Agents(game) { NONE }
      val admin = agents.world.reader
      agents.runSystem(
          "GpComplete<Class<TemperatureStep>>, " +
              "GpComplete<Class<OxygenStep>>, " +
              "GpComplete<Class<OceanTile>>, " +
              "GpComplete<Class<VenusStep>>"
      )
      admin.count("GpIncomplete") shouldBe 0

      agents.runSystem("WorldGovernmentTerraforming<Player1>")

      game.tasks.ids() shouldBe emptySet()
    }
  }
}
