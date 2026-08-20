package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TestOption.WorldGovernmentOption
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest {
  @Test
  fun `start player chooses an Engine increase that triggers Aphrodite`() {
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
  fun `a completed parameter is not a legal World Government choice`() {
    val game = setUpGame(VenusNextExpansion)
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("15 VenusStep")

    TfmWorkflow.Manual(game).solarPhase()

    shouldThrow<LimitsException> { p1.doTask("VenusStep! BY Engine") }
    p1.doTask("TemperatureStep! BY Engine")
  }

  @Test
  fun `World Government does not trigger an ordinary owner-only effect`() {
    val game = setUpGame(VenusNextExpansion, PromoCardPack)
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("$HomeostasisBureau")

    TfmWorkflow.Manual(game).solarPhase()
    p1.doTask("TemperatureStep! BY Engine")

    p1.count("Megacredit") shouldBe 0
  }

  @Test
  fun `World Government is absent without Venus or when disabled`() {
    val ordinary = setUpGame()
    TfmWorkflow.Manual(ordinary).solarPhase()
    ordinary.tasks.ids() shouldBe emptySet()

    val disabled = setUpGame(VenusNextExpansion, exclude(WorldGovernmentOption))
    TfmWorkflow.Manual(disabled).solarPhase()
    disabled.tasks.ids() shouldBe emptySet()
  }

  @Test
  fun `World Government is skipped after every parameter is complete`() {
    val game = setUpGame(VenusNextExpansion)
    val engine = game.tfm(ENGINE)
    engine
        .godMode()
        .sneak(
            "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>, " +
                "GpComplete<Class<VenusStep>>"
        )
    engine.count("LastCall") shouldBe 0

    TfmWorkflow.Manual(game).solarPhase()

    game.tasks.ids() shouldBe emptySet()
  }

  @Test
  fun `Solar phase is skipped when production ends the game`() {
    val game = setUpGame(VenusNextExpansion)
    val engine = game.tfm(ENGINE)
    engine.godMode().sneak("LastCall")

    TfmWorkflow.Manual(game).solarPhase()

    engine.count("SolarPhase") shouldBe 0
    game.tasks.ids() shouldBe emptySet()
  }
}
