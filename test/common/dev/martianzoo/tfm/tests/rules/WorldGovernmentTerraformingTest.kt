package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.engine.*
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest : TfmGameplayTest() {
  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    newTestGame()
    repeat(19) { finishGeneration("TemperatureStep") }
    repeat(8) { finishGeneration("OceanTile") }
    repeat(14) { finishGeneration("OxygenStep") }

    rob.doTask("Pass")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }

    admin.count("SolarPhase") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 1
  }

  private fun finishGeneration(parameter: String) {
    val first = players.indexOfFirst { it.count("StartToken") == 1 }
    val order = players.drop(first) + players.take(first)
    order.forEach { it.doTask("Pass") }
    players.forEach { it.autoExecNow(EAGER) }
    val temperature = admin.count("TemperatureStep")
    val choice =
        if (parameter == "OceanTile") "OceanTile<${kim.list("WaterArea(HAS MAX 0 Tile)").first()}>"
        else parameter
    order.first().doTask("$choice! BY Admin")

    if (parameter == "TemperatureStep" && temperature == 14)
        order.first().doTask("OceanTile<Tharsis_1_2> BY Admin")

    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  internal class Attribution : TfmTest() {
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
