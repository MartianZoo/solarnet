package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardsTest : TfmSandboxTest() {

  @Test
  internal fun customAwardMetricsAreCountedForEachPlayer() {
    game = TfmEngine.newGame(canonicalPremise(Cimmeria, players = 3))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    val p3 = agents[PLAYER3]

    p1.exMachina("Forecaster, $ArtificialLake")
    p2.exMachina("$Birds, $Algae")
    p1.count("CardFront(HAS requirement)") shouldBe 1
    p2.count("CardFront(HAS requirement)") shouldBe 2

    admin.beginOperation("End FROM Phase")
    p1.doTask("3 VictoryPoint")
    p2.doTask("5 VictoryPoint")

    p1.assertCounts(
        1 to "SecondPlace<Player1, Forecaster>",
    )
    p2.assertCounts(
        1 to "FirstPlace<Player2, Forecaster>",
    )
    p3.assertCounts(
        0 to "FirstPlace<Player3, Forecaster>",
        0 to "SecondPlace<Player3, Forecaster>",
    )
  }

  @Test
  internal fun zeroScoresCanEarnFirstAndSecondWhileUnfundedAwardsAreIgnored() {
    game = TfmEngine.newGame(canonicalPremise(players = 3))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    val p3 = agents[PLAYER3]

    p1.exMachina("Thermalist, Miner, Heat")

    admin.beginOperation("End FROM Phase")
    p1.doTask("10 VictoryPoint")
    p2.doTask("7 VictoryPoint")
    p3.doTask("7 VictoryPoint")

    p1.assertCounts(
        1 to "FirstPlace<Player1, Thermalist>",
        1 to "FirstPlace<Player1, Miner>",
        0 to "FirstPlace<Player1, Scientist>",
        10 to "VictoryPoint",
    )
    p2.assertCounts(
        1 to "SecondPlace<Player2, Thermalist>",
        1 to "FirstPlace<Player2, Miner>",
        0 to "FirstPlace<Player2, Scientist>",
        7 to "VictoryPoint",
    )
    p3.assertCounts(
        1 to "SecondPlace<Player3, Thermalist>",
        1 to "FirstPlace<Player3, Miner>",
        0 to "FirstPlace<Player3, Scientist>",
        7 to "VictoryPoint",
    )
  }

  @Test
  internal fun negativeBankerProductionCanEarnFirstAndSecond() {
    game = TfmEngine.newGame(canonicalPremise(players = 3))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    val p3 = agents[PLAYER3]

    p1.exMachina("Banker, PROD[-4 MC]")
    p2.exMachina("PROD[-5 MC]")
    p3.exMachina("PROD[-5 MC]")
    p1.assertProds(-4 to "MC")
    p2.assertProds(-5 to "MC")
    p3.assertProds(-5 to "MC")

    admin.beginOperation("End FROM Phase")
    p1.doTask("5 VictoryPoint")
    p2.doTask("2 VictoryPoint")
    p3.doTask("2 VictoryPoint")

    p1.assertCounts(1 to "FirstPlace<Player1, Banker>", 5 to "VictoryPoint")
    p2.assertCounts(1 to "SecondPlace<Player2, Banker>", 2 to "VictoryPoint")
    p3.assertCounts(1 to "SecondPlace<Player3, Banker>", 2 to "VictoryPoint")
  }

  @Test
  internal fun winnerWaitsForPlayersToTakeTheirScoringTasks() {
    game = TfmEngine.newGame(canonicalPremise())
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    p1.runOperation("3 VictoryPoint, TerraformRating")
    p2.runOperation("Banker, PROD[1 MC]")
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE

    admin.beginOperation("End FROM Phase")
    p1.count("VictoryPoint") shouldBe 3
    p2.count("VictoryPoint") shouldBe 0
    admin.count("GameScope") shouldBe 1
    p1.doTask("VictoryPoint")
    p2.count("Victory") shouldBe 0
    p2.doTask("5 VictoryPoint")

    p1.assertCounts(4 to "VictoryPoint<Player1>", 0 to "Victory<Player1>")
    p2.assertCounts(5 to "VictoryPoint<Player2>", 1 to "Victory<Player2>")
    admin.count("End") shouldBe 1
    admin.count("GameScope") shouldBe 0
    admin.count("MeasureAward<Banker>") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun playersTakeNetScoresIncludingNegativeCardsAndPlayedEvents() {
    game = TfmEngine.newGame(canonicalPremise())
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val p2 = agents[PLAYER2].also { it.autoExecPolicy = NONE }
    p1.exMachina("$CorporateStronghold, $EarthCatapult, 5 TerraformRating")
    p2.exMachina("$EnergyTapping, PlayedEvent<Class<$LargeConvoy>>, 3 TerraformRating")

    admin.beginOperation("End FROM Phase")
    p1.count("VictoryPoint") shouldBe 0
    p2.count("VictoryPoint") shouldBe 0
    p1.doTask("5 VictoryPoint")
    p2.doTask("4 VictoryPoint")

    p1.count("VictoryPoint") shouldBe 5
    p2.count("VictoryPoint") shouldBe 4
    game.isIdle() shouldBe true
  }

  @Test
  internal fun fundingPriceProgressesAndOnlyThreeAwardsCanBeFunded() {
    newTestGame(playerCount = 2)
    kim.setToExMachina(100, "MC")
    kim.inTurn {
      doTask("UseAction<FundAwardAction, Action1>")
      shouldThrow<TaskException> { doTask("Landlord") }
      doTasks("8 Pay<> FROM MC", "Landlord")
    }
    kim.assertCounts(92 to "MC", 1 to "Landlord")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Landlord")
      }
    }
    kim.assertCounts(92 to "MC", 1 to "Landlord")

    kim.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Scientist")
    }
    kim.assertCounts(78 to "MC", 1 to "Scientist")

    kim.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Thermalist")
    }
    kim.assertCounts(58 to "MC", 1 to "Thermalist", 3 to "Award")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks("UseAction<FundAwardAction, Action1>", "Miner")
      }
    }
    kim.assertCounts(58 to "MC", 3 to "Award", 0 to "Miner")
  }

  @Test
  internal fun `Tied players receive the appropriate first and second place award points`() {
    newTestGame()
    kim.setToExMachina(3, "Heat")
    kim.setToExMachina(3, "Steel")
    stan.setToExMachina(2, "Heat")
    stan.setToExMachina(3, "Steel")
    rob.setToExMachina(2, "Heat")
    rob.setToExMachina(2, "Steel")
    kim.inTurn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Thermalist",
      )
    }
    stan.inTurn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "14 Pay<> FROM MC",
          "Miner",
      )
    }

    victoryPoints() shouldBe listOf(30, 27, 22)
  }

  @Test
  internal fun `A two-player game awards no second-place points`() {
    newTestGame(playerCount = 2)
    kim.setToExMachina(1, "Heat")
    kim.inTurn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Thermalist",
      )
    }

    victoryPoints() shouldBe listOf(25, 20)
  }
}
