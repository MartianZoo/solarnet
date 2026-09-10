package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest {
  @Test
  internal fun `start player chooses an Admin increase that triggers Aphrodite`() {
    val game = setUpGame(VenusNextExpansion, players = 3)
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.runOperation("$Aphrodite")
    val mcBefore = p1.count("MC")
    admin.runOperation("StartToken<Player2> FROM StartToken<Player1>")
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Stepwise(game).solarPhase()

    admin.count("SolarPhase") shouldBe 1
    p2.doTask("VenusStep! BY Admin")

    val venusIncrease =
        game.events.changesSince(checkpoint).single {
          it.change.gaining?.className.toString() == "VenusStep"
        }
    venusIncrease.actor shouldBe ADMIN
    p2.count("TerraformRating") shouldBe 20
    p1.count("MC") shouldBe mcBefore + 2
  }

  @Test
  internal fun `World Government is skipped after every parameter is complete`() {
    val game = setUpGame(VenusNextExpansion)
    val admin = game.tfm(ADMIN)
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, " +
            "GpComplete<Class<VenusStep>>"
    )
    admin.count("GpIncomplete") shouldBe 0

    TfmWorkflow.Stepwise(game).solarPhase()

    game.tasks.ids() shouldBe emptySet()
  }

  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    val game = setUpGame(VenusNextExpansion)
    val admin = game.tfm(ADMIN)
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    TfmWorkflow.Stepwise(game).solarPhase()

    admin.count("SolarPhase") shouldBe 0
    game.tasks.ids() shouldBe emptySet()
  }
}
