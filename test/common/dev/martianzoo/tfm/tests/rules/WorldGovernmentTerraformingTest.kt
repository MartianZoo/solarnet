package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest : TfmGameplayTest() {
  @Test
  internal fun `start player chooses an Admin increase that triggers Aphrodite`() {
    newTestGame(kimCorporation = Aphrodite)
    admin.sneak("StartToken<Stan> FROM StartToken<Kim>")
    kim.pass()
    stan.pass()
    rob.pass()
    val mcBefore = kim.count("MC")
    val checkpoint = game.timeline.checkpoint()

    admin.count("SolarPhase") shouldBe 1
    stan.wgt("VenusStep")

    val venusIncrease =
        game.events.changesSince(checkpoint).single {
          it.change.gaining?.className.toString() == "VenusStep"
        }
    venusIncrease.actor shouldBe ADMIN
    stan.count("TerraformRating") shouldBe 20
    kim.count("MC") shouldBe mcBefore + 2
  }

  @Test
  internal fun `first player chooses the threshold ocean after Admin raises temperature`() {
    val game = setUpGame(VenusNextExpansion, players = 2)
    val admin = game.testTfm(ADMIN)
    val p2 = game.testTfm(PLAYER2)
    admin.runOperation("StartToken<Player2> FROM StartToken<Player1>")
    admin.runOperation("14 TemperatureStep")
    val checkpoint = game.timeline.checkpoint()

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
    val admin = game.testTfm(ADMIN)
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

  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    newTestGame()
    admin.sneak(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    kim.pass()
    stan.pass()
    rob.pass()

    admin.count("SolarPhase") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 1
  }
}
