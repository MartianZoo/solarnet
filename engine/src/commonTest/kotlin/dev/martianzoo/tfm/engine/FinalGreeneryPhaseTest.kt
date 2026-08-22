package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FinalGreeneryPhaseTest {
  @Test
  fun normalGreeneryRaisesOxygen() {
    val game = Engine.newGame(canonicalPremise())
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(Ecoline, 0)
    game.tfm(PLAYER2).playCorp(TharsisRepublic, 0)
    p1.godMode().sneak("8 Plant")

    p1.convertPlants {
      doTask("GreeneryTile<Tharsis_3_6>")
    }

    engine.oxygenPercent() shouldBe 1
    workflow.shutdown()
  }

  @Test
  fun finalGreeneryRaisesOxygen() {
    val game = Engine.newGame(canonicalPremise())
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Manual(game)

    workflow.setupPhase()
    workflow.corporationPhase()
    p1.godMode().manual("8 Plant")
    workflow.finalGreeneryPhase()
    p1.startTurn()
    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_5>") }

    engine.oxygenPercent() shouldBe 1
  }

  @Test
  fun automaticSoloLossSkipsFinalGreeneryAndScoring() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.playCorp(Ecoline, 0)
    engine.godMode().sneak("-13 SoloGenerationsLeft, LastCall")

    p1.pass()

    engine.count("FinalGreeneryPhase") shouldBe 0
    engine.count("EndPhase") shouldBe 0
    engine.count("Victory<Me>") shouldBe 0
    engine.count("TemperatureStep") shouldBe 0
    engine.count("OxygenStep") shouldBe 0
    engine.count("OceanTile") shouldBe 0
    workflow.isRunning shouldBe false
    workflow.shutdown()
  }

  @Test
  fun automaticSoloWinRequiresCompletedBaseParameters() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.playCorp(Ecoline, 0)
    engine
        .godMode()
        .sneak(
            "-13 SoloGenerationsLeft, LastCall, " +
                "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>"
        )

    p1.pass()

    engine.count("Victory<Me>") shouldBe 1
    engine.count("FinalGreeneryPhase") shouldBe 1
    workflow.shutdown()
  }

  @Test
  fun venusSoloAlsoRequiresCompletedVenusParameter() {
    val setup = canonicalPremise(VenusNextExpansion, players = 1)
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.playCorp(Ecoline, 0)
    engine
        .godMode()
        .sneak(
            "-13 SoloGenerationsLeft, LastCall, " +
                "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>"
        )

    p1.pass()

    engine.count("FinalGreeneryPhase") shouldBe 0
    engine.count("EndPhase") shouldBe 0
    engine.count("Victory<Me>") shouldBe 0
    workflow.isRunning shouldBe false
    workflow.shutdown()
  }

  @Test
  fun automaticMultiplayerDoesNotTreatAbsentCountdownAsGameEnd() {
    val setup = canonicalPremise()
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(Ecoline, 0)
    p2.playCorp(TharsisRepublic, 0)
    p1.pass()
    p2.pass()

    game.classTable.allClassNames.shouldNotContain(cn("SoloGenerationsLeft"))
    engine.count("ResearchPhase") shouldBe 1
    engine.count("FinalGreeneryPhase") shouldBe 0
    workflow.shutdown()
  }

  @Test
  fun multiplayerCompletesOneFinalProductionBeforeFinalGreenery() {
    val game = Engine.newGame(canonicalPremise())
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(Ecoline, 0)
    p2.playCorp(TharsisRepublic, 0)
    p1.godMode().sneak("PROD[Steel]")
    engine
        .godMode()
        .sneak(
            "LastCall, GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>"
        )

    p1.pass()
    p2.pass()

    p1.count("Steel<Player1>") shouldBe 1
    engine.count("FinalGreeneryPhase") shouldBe 1
    workflow.shutdown()
  }

  @Test
  fun multiplayerFinalGreeneryAdvancesAfterAPlayerCanNoLongerConvert() {
    val game = Engine.newGame(canonicalPremise(players = 3))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val p3 = game.tfm(PLAYER3)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p3.playCorp(InterplanetaryCinematics, 0)
    p1.godMode().sneak("8 Plant")
    p2.godMode().sneak("8 Plant")
    p3.godMode().sneak("8 Plant")
    engine
        .godMode()
        .sneak(
            "LastCall, GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
        )

    p1.pass()
    p2.pass()
    p3.pass()
    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_5>") }
    p1.doTask("Ok")
    p2.convertPlants { doTask("GreeneryTile<Tharsis_3_6>") }
    p2.doTask("Ok")
    p3.convertPlants { doTask("GreeneryTile<Tharsis_3_7>") }
    p3.doTask("Ok")

    p1.count("GreeneryTile<Player1>") shouldBe 1
    p2.count("GreeneryTile<Player2>") shouldBe 1
    p3.count("GreeneryTile<Player3>") shouldBe 1
    workflow.shutdown()
  }

  @Test
  fun tenPlantsCanBecomeTwoGreeneriesWithEcolinePolderTechAndTheElysiumBonus() {
    val game = Engine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p1.godMode().sneak("$Ecoline, $PolderTechDutch, 10 Plant")
    engine
        .godMode()
        .sneak(
            "LastCall, GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
        )

    p1.pass()
    p2.pass()
    p1.count("Plant") shouldBe 10
    p1.convertPlants {
      // 10 - 7 with Ecoline + 1 from PolderTECH + the unique 3-plant bonus = 7.
      doTask("GreeneryTile<Elysium_5_6>")
    }
    p1.count("Plant") shouldBe 7
    p1.convertPlants { doTask("GreeneryTile<Elysium_5_5>") }
    p1.doTask("Ok")

    p1.count("GreeneryTile<Player1>") shouldBe 2
    workflow.shutdown()
  }

  @Test
  fun tenPlantsCanBecomeTwoGreeneriesWithPhilaresNeighborsAndTheElysiumBonus() {
    val game = Engine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p1.godMode().sneak("GreeneryTile<Elysium_4_5>")
    p2.godMode()
        .sneak(
            "GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, " + "GreeneryTile<Elysium_6_6>"
        )
    p1.godMode().sneak("$Philares, 10 Plant")
    engine
        .godMode()
        .sneak(
            "LastCall, GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
        )

    p1.pass()
    p2.pass()
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
  fun sevenPlantsCanBecomeTwoGreeneriesInTheMostContrivedCanonicalCase() {
    val game = Engine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p1.godMode().sneak("GreeneryTile<Elysium_4_5>")
    p2.godMode()
        .sneak(
            "GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, " +
                "GreeneryTile<Elysium_6_6>, GreeneryTile<Elysium_6_7>"
        )
    p1.godMode().sneak("$Ecoline, $Philares, 7 Plant")
    engine
        .godMode()
        .sneak(
            "LastCall, GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
        )

    p1.pass()
    p2.pass()
    p1.count("Plant") shouldBe 7
    p1.count("GreeneryTile<Player1>") shouldBe 1
    p1.convertPlants {
      // Ecoline makes 7 payable; the 3-plant bonus plus four Philares plants restores all 7.
      doTask("GreeneryTile<Elysium_5_6>")
      repeat(4) { doTask("Plant") }
    }
    p1.count("Plant") shouldBe 7
    p1.convertPlants { doTask("GreeneryTile<Elysium_3_4>") }
    p1.doTask("Ok")

    p1.count("GreeneryTile<Player1>") shouldBe 3
    workflow.shutdown()
  }

  @Test
  fun multiplayerEndConditionIgnoresVenusCompletion() {
    val game = setUpGame(VenusNextExpansion)
    val engine = game.tfm(ENGINE)

    engine
        .godMode()
        .manual(
            "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>"
        )

    engine.count("GpComplete<Class<VenusStep>>") shouldBe 0
    engine.count("LastCall") shouldBe 1
  }
}
