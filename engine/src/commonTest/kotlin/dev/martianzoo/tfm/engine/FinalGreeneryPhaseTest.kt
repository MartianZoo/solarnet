package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
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

    p1.playCorp("Ecoline", 0)
    game.tfm(PLAYER2).playCorp("TharsisRepublic", 0)
    p1.godMode().sneak("8 Plant")
    engine.count("Photosynthesis") shouldBe 1

    p1.stdAction("ConvertPlantsSA") {
      doTask("GreeneryTile<Tharsis_3_6>")
    }

    engine.oxygenPercent() shouldBe 1
    workflow.shutdown()
  }

  @Test
  fun finalGreeneryDoesNotRaiseOxygen() {
    val game = Engine.newGame(canonicalPremise())
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Manual(game)

    workflow.setupPhase()
    workflow.corporationPhase()
    p1.godMode().manual("8 Plant")
    workflow.finalGreeneryPhase()
    engine.count("Photosynthesis") shouldBe 0
    p1.startTurn()
    p1.doTask("UseAction1<ConvertPlantsSA>")
    p1.doTask("GreeneryTile<Tharsis_3_5>")

    engine.oxygenPercent() shouldBe 0
  }

  @Test
  fun automaticSoloLossSkipsFinalGreeneryAndScoring() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game).launch()

    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
    p1.playCorp("Ecoline", 0)
    engine.godMode().sneak("-13 SoloGenerationsLeft, LastCall")

    p1.pass()

    engine.count("FinalGreeneryPhase") shouldBe 0
    engine.count("EndPhase") shouldBe 0
    engine.count("Victory<Player1>") shouldBe 0
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

    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
    p1.playCorp("Ecoline", 0)
    engine
        .godMode()
        .sneak(
            "-13 SoloGenerationsLeft, LastCall, " +
                "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>"
        )

    p1.pass()

    engine.count("Victory<Player1>") shouldBe 1
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

    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
    p1.playCorp("Ecoline", 0)
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
    engine.count("Victory<Player1>") shouldBe 0
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

    p1.playCorp("Ecoline", 0)
    p2.playCorp("TharsisRepublic", 0)
    p1.pass()
    p2.pass()

    game.classTable.allClassNamesAndIds.shouldNotContain(cn("SoloGenerationsLeft"))
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

    p1.playCorp("Ecoline", 0)
    p2.playCorp("TharsisRepublic", 0)
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
