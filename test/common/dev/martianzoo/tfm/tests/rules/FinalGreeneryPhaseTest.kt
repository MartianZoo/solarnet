package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FinalGreeneryPhaseTest {
  @Test
  internal fun normalGreeneryRaisesOxygen() {
    val game = Engine.newGame(canonicalPremise())
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(Ecoline, 0)
    game.tfm(PLAYER2).playCorp(TharsisRepublic, 0)
    p1.sneak("8 Plant")

    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_6>") }

    admin.oxygenPercent() shouldBe 1
    workflow.shutdown()
  }

  @Test
  internal fun finalGreeneryDoesNotRaiseOxygen() {
    val game = Engine.newGame(canonicalPremise())
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Manual(game)

    workflow.setupPhase()
    workflow.corporationPhase()
    p1.manual("8 Plant")
    workflow.finalGreeneryPhase()
    p1.startTurn()
    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_5>") }

    admin.oxygenPercent() shouldBe 0
  }

  @Test
  internal fun automaticSoloLossSkipsFinalGreeneryAndScoring() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.playCorp(Ecoline, 0)
    admin.sneak("-13 SoloGenerationsLeft")

    p1.pass()

    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("End") shouldBe 0
    admin.count("Victory<Me>") shouldBe 0
    admin.count("TemperatureStep") shouldBe 0
    admin.count("OxygenStep") shouldBe 0
    admin.count("OceanTile") shouldBe 0
    workflow.isRunning shouldBe false
    workflow.shutdown()
  }

  @Test
  internal fun automaticSoloWinRequiresCompletedBaseParameters() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.playCorp(Ecoline, 0)
    admin.sneak(
        "-13 SoloGenerationsLeft, " +
            "GpComplete<Class<TemperatureStep>> FROM GpIncomplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>> FROM GpIncomplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>> FROM GpIncomplete<Class<OceanTile>>"
    )

    p1.pass()

    admin.count("Victory<Me>") shouldBe 1
    admin.count("FinalGreeneryPhase") shouldBe 1
    workflow.shutdown()
  }

  @Test
  internal fun venusSoloAlsoRequiresCompletedVenusParameter() {
    val setup = canonicalPremise(VenusNextExpansion, players = 1)
    val game = Engine.newGame(setup)
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    p1.playCorp(Ecoline, 0)
    admin.sneak(
        "-13 SoloGenerationsLeft, " +
            "GpComplete<Class<TemperatureStep>> FROM GpIncomplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>> FROM GpIncomplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>> FROM GpIncomplete<Class<OceanTile>>"
    )

    p1.pass()

    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("End") shouldBe 0
    admin.count("Victory<Me>") shouldBe 0
    workflow.isRunning shouldBe false
    workflow.shutdown()
  }

  @Test
  internal fun automaticMultiplayerDoesNotTreatAbsentCountdownAsGameEnd() {
    val setup = canonicalPremise()
    val game = Engine.newGame(setup)
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(Ecoline, 0)
    p2.playCorp(TharsisRepublic, 0)
    p1.pass()
    p2.pass()

    game.classTable.allClassNames.shouldNotContain(cn("SoloGenerationsLeft"))
    admin.count("ResearchPhase") shouldBe 1
    admin.count("FinalGreeneryPhase") shouldBe 0
    workflow.shutdown()
  }

  @Test
  internal fun multiplayerFinalGreeneryAdvancesAfterAPlayerCanNoLongerConvert() {
    val game = Engine.newGame(canonicalPremise(players = 3))
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val p3 = game.tfm(PLAYER3)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p3.playCorp(InterplanetaryCinematics, 0)
    p1.sneak("8 Plant")
    p2.sneak("8 Plant")
    p3.sneak("8 Plant")
    admin.sneak(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>, " +
            "GpComplete<Class<TemperatureStep>> FROM GpIncomplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>> FROM GpIncomplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>> FROM GpIncomplete<Class<OceanTile>>"
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
  internal fun tenPlantsCanBecomeTwoGreeneriesWithEcolinePolderTechAndTheElysiumBonus() {
    val game = Engine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p1.sneak("$Ecoline, $PolderTechDutch, 10 Plant")
    admin.sneak(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>, " +
            "GpComplete<Class<TemperatureStep>> FROM GpIncomplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>> FROM GpIncomplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>> FROM GpIncomplete<Class<OceanTile>>"
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
  internal fun tenPlantsCanBecomeTwoGreeneriesWithPhilaresNeighborsAndTheElysiumBonus() {
    val game = Engine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p1.sneak("GreeneryTile<Elysium_4_5>")
    p2.sneak("GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, " + "GreeneryTile<Elysium_6_6>")
    p1.sneak("$Philares, 10 Plant")
    admin.sneak(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>, " +
            "GpComplete<Class<TemperatureStep>> FROM GpIncomplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>> FROM GpIncomplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>> FROM GpIncomplete<Class<OceanTile>>"
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
  internal fun sevenPlantsCanBecomeTwoGreeneriesInTheMostContrivedCanonicalCase() {
    val game = Engine.newGame(canonicalPremise(Elysium, PromoCardPack))
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(CrediCor, 0)
    p2.playCorp(MiningGuild, 0)
    p1.sneak("GreeneryTile<Elysium_4_5>")
    p2.sneak(
        "GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, " +
            "GreeneryTile<Elysium_6_6>, GreeneryTile<Elysium_6_7>"
    )
    p1.sneak("$Ecoline, $Philares, 7 Plant")
    admin.sneak(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>, " +
            "GpComplete<Class<TemperatureStep>> FROM GpIncomplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>> FROM GpIncomplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>> FROM GpIncomplete<Class<OceanTile>>"
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
  internal fun multiplayerEndConditionIgnoresVenusCompletion() {
    val game = setUpGame(VenusNextExpansion)
    val admin = game.tfm(ADMIN)

    admin.manual(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    admin.count("GpComplete<Class<VenusStep>>") shouldBe 0
    admin.count("GameEndBarrier") shouldBe 0
  }

  @Test
  internal fun mandatoryVenusVariantKeepsItsOwnBarrierUntilVenusIsComplete() {
    val game = setUpGame(VenusNextExpansion, MandatoryVenusVariant)
    val admin = game.tfm(ADMIN)

    admin.manual(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    admin.count("GameEndBarrier") shouldBe 1

    admin.manual("GpComplete<Class<VenusStep>>")

    admin.count("GameEndBarrier") shouldBe 0
  }
}
