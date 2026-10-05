package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentTerraformingTest {
  @Test
  internal fun `start player chooses an Admin increase that triggers Aphrodite`() {
    val game = setUpGame(VenusNextExpansion, players = 3)
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.runOperation("$Aphrodite")
    val mcBefore = p1.count("MC")
    admin.runOperation("StartToken<Player2> FROM StartToken<Player1>")
    val checkpoint = game.timeline.checkpoint()

    with(game.testAgents()[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }

    admin.count("VenusSolarPhase") shouldBe 1
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
    val admin = game.testTfm(ADMIN)
    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, " +
            "GpComplete<Class<VenusStep>>"
    )
    admin.count("GpIncomplete") shouldBe 0

    with(game.testAgents()[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }

    game.tasks.ids() shouldBe emptySet()
  }

  @Test
  internal fun `Solar phase is skipped when production ends the game`() {
    val game = Engine.newGame(canonicalPremise(VenusNextExpansion))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.runOperation("19 TemperatureStep") {
      p1.doTask("OceanTile<Tharsis_1_2> BY Admin")
    }
    admin.runOperation("14 OxygenStep")
    listOf("1_4", "1_5", "2_6", "4_8", "5_4", "5_5", "5_6", "6_6").forEach {
      admin.runOperation("OceanTile<Tharsis_$it>")
    }
    admin.beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, cn("CrediCor"))
    playCorporationWithoutStartingProjects(p2, cn("InterplanetaryCinematics"))

    p1.pass()
    p2.pass()

    admin.count("SolarPhase") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 1
    p1.doTask("FinishFinalGreenery")
    p2.doTask("FinishFinalGreenery")
    admin.count("End") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }
}
