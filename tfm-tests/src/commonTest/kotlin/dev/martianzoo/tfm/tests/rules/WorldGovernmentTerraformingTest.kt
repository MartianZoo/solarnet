package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest {
  @Test
  internal fun `start player chooses an Engine increase that triggers Aphrodite`() {
    val game = setUpGame(VenusNextExpansion, players = 3)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.godMode().manual("$Aphrodite")
    val megacreditsBefore = p1.count("Megacredit")
    engine.godMode().manual("StartToken<Player2> FROM StartToken<Player1>")
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game).solarPhase()

    engine.count("SolarPhase") shouldBe 1
    game.tasks
        .extract { it.assignee to it.instruction.toString() }
        .shouldContainExactly(PLAYER2 to "GlobalParameter! BY Engine")
    p2.doTask("VenusStep! BY Engine")

    val venusIncrease =
        game.events.changesSince(checkpoint).single {
          it.change.gaining?.className.toString() == "VenusStep"
        }
    venusIncrease.actor shouldBe ENGINE
    p2.count("TerraformRating") shouldBe 20
    p1.count("Megacredit") shouldBe megacreditsBefore + 2
  }

  @Test
  internal fun `World Government is skipped after every parameter is complete`() {
    val game = setUpGame(VenusNextExpansion)
    val engine = game.tfm(ENGINE)
    engine
        .godMode()
        .manual(
            "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>, " +
                "GpComplete<Class<VenusStep>>"
        )
    engine.count("GpIncomplete") shouldBe 0

    TfmWorkflow.Manual(game).solarPhase()

    game.tasks.ids() shouldBe emptySet()
  }

  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    val game = setUpGame(VenusNextExpansion)
    val engine = game.tfm(ENGINE)
    engine
        .godMode()
        .manual(
            "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
        )

    TfmWorkflow.Manual(game).solarPhase()

    engine.count("SolarPhase") shouldBe 0
    game.tasks.ids() shouldBe emptySet()
  }
}
