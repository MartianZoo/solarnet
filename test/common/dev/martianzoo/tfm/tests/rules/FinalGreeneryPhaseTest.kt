package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FinalGreeneryPhaseTest {
  @Test
  internal fun normalGreeneryRaisesOxygen() {
    val game = TfmEngine.newGame(canonicalPremise())
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("8 Plant")
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(0)
    game.testTfm(PLAYER2).keepStartingProjects(0)
    playCorporationWithoutStartingProjects(p1, Ecoline)
    playCorporationWithoutStartingProjects(game.testTfm(PLAYER2), TharsisRepublic)

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_6>") }

    admin.oxygenPercent() shouldBe 1
    workflow.shutdown()
  }

  @Test
  internal fun finalGreeneryDoesNotRaiseOxygen() {
    val game = TfmEngine.newGame(canonicalPremise())
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val workflow = TfmWorkflow.Stepwise(game.testAgents())

    workflow.setupPhase()
    p1.keepStartingProjects(0)
    game.testTfm(PLAYER2).keepStartingProjects(0)
    workflow.corporationPhase()
    p1.runOperation("8 Plant")
    workflow.finalGreeneryPhase()
    p1.startTurn()
    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_5>") }

    admin.oxygenPercent() shouldBe 0
  }

  @Test
  internal fun automaticSoloLossSkipsFinalGreeneryAndScoring() {
    val setup = canonicalPremise(players = 1)
    val game = TfmEngine.newGame(setup)
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    admin.runOperation("-13 SoloGenerationsLeft")
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.keepStartingProjects(0)
    playCorporationWithoutStartingProjects(p1, Ecoline)

    p1.pass()

    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("End") shouldBe 0
    admin.count("Victory<Player1>") shouldBe 0
    admin.count("TemperatureStep") shouldBe 0
    admin.count("OxygenStep") shouldBe 0
    admin.count("OceanTile") shouldBe 0
    workflow.isRunning shouldBe false
    workflow.shutdown()
  }

  @Test
  internal fun automaticMultiplayerDoesNotTreatAbsentCountdownAsGameEnd() {
    val setup = canonicalPremise()
    val game = TfmEngine.newGame(setup)
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
    playCorporationWithoutStartingProjects(p1, Ecoline)
    playCorporationWithoutStartingProjects(p2, TharsisRepublic)
    p1.pass()
    p2.pass()

    game.classTable.allClassNames.shouldNotContain(cn("SoloGenerationsLeft"))
    admin.count("ResearchPhase") shouldBe 1
    admin.count("FinalGreeneryPhase") shouldBe 0
    workflow.shutdown()
  }

  @Test
  internal fun elevenPlantsCanBecomeTwoGreeneriesWithEcolineAndTheElysiumBonus() {
    val game = TfmEngine.newGame(canonicalPremise(Elysium))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.runOperation("6 Plant")
    admin.runOperation(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>"
    )
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
    playCorporationWithoutStartingProjects(p1, Ecoline)
    playCorporationWithoutStartingProjects(p2, MiningGuild)

    p1.pass()
    p2.pass()
    p1.count("Plant") shouldBe 11
    p1.convertPlants {
      // 11 - 7 with Ecoline + the unique 3-plant bonus = 7.
      doTask("GreeneryTile<Elysium_5_6>")
    }
    p1.count("Plant") shouldBe 7
    p1.convertPlants { doTask("GreeneryTile<Elysium_5_5>") }
    p1.doTask("Ok")

    p1.count("GreeneryTile<Player1>") shouldBe 2
    workflow.shutdown()
  }

  @Test
  internal fun tenPlantsCanBecomeTwoGreeneriesWithPhilaresNeighborsAndTheElysiumBonus() {
    val game = TfmEngine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p2.runOperation(
        "GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, " + "GreeneryTile<Elysium_6_6>"
    )
    p1.runOperation("8 Plant")
    admin.runOperation(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>"
    )
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
    playCorporationWithoutStartingProjects(p1, Philares)
    playCorporationWithoutStartingProjects(p2, MiningGuild)

    p1.turn {
      stdAction("RequiredActionsSignal") {
        doTask("GreeneryTile<Elysium_4_5>")
        doTask("Plant")
      }
    }
    p2.pass()
    p1.pass()
    p1.count("Plant") shouldBe 10
    p1.count("GreeneryTile<Player1>") shouldBe 1
    p1.convertPlants {
      // 10 - 8 + the 3-plant bonus + one Philares plant per opponent adjacency = 8.
      doTask("GreeneryTile<Elysium_5_6>")
      repeat(3) { doTask("Plant") }
    }
    p1.count("Plant") shouldBe 8
    p1.convertPlants {
      doTask("GreeneryTile<Elysium_6_7>")
      repeat(2) { doTask("Plant") }
    }
    p1.doTask("Ok")

    p1.count("GreeneryTile<Player1>") shouldBe 3
    workflow.shutdown()
  }

  @Test
  internal fun multiplayerEndConditionIgnoresVenusCompletion() {
    val game = setUpGame(VenusNextExpansion)
    val admin = game.testTfm(ADMIN)

    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    admin.count("GpComplete<Class<VenusStep>>") shouldBe 0
    admin.count("GameEndBarrier") shouldBe 0
  }

  @Test
  internal fun mandatoryVenusVariantKeepsItsOwnBarrierUntilVenusIsComplete() {
    val game = setUpGame(VenusNextExpansion, MandatoryVenusVariant)
    val admin = game.testTfm(ADMIN)

    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    admin.count("GameEndBarrier") shouldBe 1

    admin.runOperation("GpComplete<Class<VenusStep>>")

    admin.count("GameEndBarrier") shouldBe 0
  }
}
