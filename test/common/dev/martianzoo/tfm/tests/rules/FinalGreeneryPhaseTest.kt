package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FinalGreeneryPhaseTest : TfmSandboxTest() {
  @Test
  internal fun normalGreeneryRaisesOxygen() {
    game = TfmEngine.newGame(canonicalPremise())
    val p1 = agents[PLAYER1]
    p1.runOperation("8 Plant")
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.doTask("-10 ProjectCard<Selecting>")
    agents[PLAYER2].doTask("-10 ProjectCard<Selecting>")
    p1.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$Ecoline>>") }
    agents[PLAYER2].completeOperation {
      doTask("PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>")
    }

    p1.completeOperation {
      doWithAutoExecPolicy(NONE) {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "7 Pay<Class<Plant>> FROM Plant")
      }
      autoExecNow()
      doTask("GreeneryTile<Tharsis_3_6>")
    }

    admin.count("OxygenStep") shouldBe 1
  }

  @Test
  internal fun finalGreeneryDoesNotRaiseOxygen() {
    game = TfmEngine.newGame(canonicalPremise())
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    p1.runOperation("8 Plant")
    admin.runOperation(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>"
    )
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.doTask("-10 ProjectCard<Selecting>")
    p2.doTask("-10 ProjectCard<Selecting>")
    p1.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$Ecoline>>") }
    p2.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>") }
    p1.doTask("Pass")
    p2.doTask("Pass")

    admin.count("FinalGreeneryPhase") shouldBe 1
    p1.completeOperation {
      doWithAutoExecPolicy(NONE) {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "7 Pay<Class<Plant>> FROM Plant")
      }
      autoExecNow()
      doTask("GreeneryTile<Tharsis_3_5>")
    }

    admin.count("OxygenStep") shouldBe 0
  }

  @Test
  internal fun soloLossSkipsFinalGreeneryAndScoring() {
    val setup = canonicalPremise(players = 1)
    game = TfmEngine.newGame(setup)
    val p1 = agents[PLAYER1]
    admin.runOperation("-13 SoloGenerationsLeft")
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.doTask("-10 ProjectCard<Selecting>")
    p1.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$Ecoline>>") }

    p1.doTask("Pass")

    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("End") shouldBe 0
    admin.count("Victory<Player1>") shouldBe 0
    admin.count("GameScope") shouldBe 0
    admin.count("TemperatureStep") shouldBe 0
    admin.count("OxygenStep") shouldBe 0
    admin.count("OceanTile") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.events.changesSinceSetup().last().change.removing?.type?.className shouldBe cn("GameScope")
  }

  @Test
  internal fun multiplayerDoesNotTreatAbsentCountdownAsGameEnd() {
    val setup = canonicalPremise()
    game = TfmEngine.newGame(setup)
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.doTask("-10 ProjectCard<Selecting>")
    p2.doTask("-10 ProjectCard<Selecting>")
    p1.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$Ecoline>>") }
    p2.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>") }
    p1.doTask("Pass")
    p2.doTask("Pass")

    game.classTable.allClassNames.shouldNotContain(cn("SoloGenerationsLeft"))
    admin.count("ResearchPhase") shouldBe 1
    admin.count("FinalGreeneryPhase") shouldBe 0
  }

  @Test
  internal fun elevenPlantsCanBecomeTwoGreeneriesWithEcolineAndTheElysiumBonus() {
    game = TfmEngine.newGame(canonicalPremise(Elysium))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    p1.runOperation("6 Plant")
    admin.runOperation(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>"
    )
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.doTask("-10 ProjectCard<Selecting>")
    p2.doTask("-10 ProjectCard<Selecting>")
    p1.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$Ecoline>>") }
    p2.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$MiningGuild>>") }

    p1.doTask("Pass")
    p2.doTask("Pass")
    p1.count("Plant") shouldBe 11
    p1.completeOperation {
      doWithAutoExecPolicy(NONE) {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "7 Pay<Class<Plant>> FROM Plant")
      }
      autoExecNow()

      // 11 - 7 with Ecoline + the unique 3-plant bonus = 7.
      doTask("GreeneryTile<Elysium_5_6>")
    }
    p1.count("Plant") shouldBe 7
    p1.completeOperation {
      doWithAutoExecPolicy(NONE) {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "7 Pay<Class<Plant>> FROM Plant")
      }
      autoExecNow()
      doTask("GreeneryTile<Elysium_5_5>")
    }
    p1.selectTask("PassBaton")

    p1.count("GreeneryTile<Player1>") shouldBe 2
  }

  @Test
  internal fun tenPlantsCanBecomeTwoGreeneriesWithPhilaresNeighborsAndTheElysiumBonus() {
    game = TfmEngine.newGame(canonicalPremise(Elysium, PromoCards))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    p2.runOperation(
        "GreeneryTile<Elysium_5_5>, GreeneryTile<Elysium_5_7>, " + "GreeneryTile<Elysium_6_6>"
    )
    p1.runOperation("8 Plant")
    admin.runOperation(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, " +
            "-GpGameEndBarrier<Class<OceanTile>>"
    )
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.doTask("-10 ProjectCard<Selecting>")
    p2.doTask("-10 ProjectCard<Selecting>")
    p1.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$Philares>>") }
    p2.completeOperation { doTask("PlayCard<Class<CorporationCard>, Class<$MiningGuild>>") }

    p1.completeOperation {
      doTask("DoRequiredActions")
      doTask("GreeneryTile<Elysium_4_5>")
      doTask("Plant")
    }
    p1.selectTask("PassBaton")
    p2.doTask("Pass")
    p1.doTask("Pass")
    p1.count("Plant") shouldBe 10
    p1.count("GreeneryTile<Player1>") shouldBe 1
    p1.completeOperation {
      doWithAutoExecPolicy(NONE) {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      }
      autoExecNow()

      // 10 - 8 + the 3-plant bonus + one Philares plant per opponent adjacency = 8.
      doTask("GreeneryTile<Elysium_5_6>")
      repeat(3) { doTask("Plant") }
    }
    p1.count("Plant") shouldBe 8
    p1.completeOperation {
      doWithAutoExecPolicy(NONE) {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      }
      autoExecNow()

      doTask("GreeneryTile<Elysium_6_7>")
      repeat(2) { doTask("Plant") }
    }
    p1.selectTask("PassBaton")

    p1.count("GreeneryTile<Player1>") shouldBe 3
  }

  @Test
  internal fun multiplayerEndConditionIgnoresVenusCompletion() {
    game = setUpGame(VenusNextExpansion)

    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    admin.count("GpComplete<Class<VenusStep>>") shouldBe 0
    admin.count("GameEndBarrier") shouldBe 0
  }

  @Test
  internal fun mandatoryVenusVariantKeepsItsOwnBarrierUntilVenusIsComplete() {
    game = setUpGame(VenusNextExpansion, MandatoryVenusVariant)

    admin.runOperation(
        "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, GpComplete<Class<OceanTile>>"
    )

    admin.count("GameEndBarrier") shouldBe 1

    admin.runOperation("GpComplete<Class<VenusStep>>")

    admin.count("GameEndBarrier") shouldBe 0
  }
}
