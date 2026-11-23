package dev.martianzoo.tfm.tests

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class TurnBlockTest : TfmGameplayTest() {
  @Test
  fun singleActionHandsOverWithoutPassing() {
    newTestGame(playerCount = 2)
    kim.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    }
    kim.assertCounts(0 to "Baton", 1 to "InRound", 0 to "Pass")
    stan.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    }
    kim.assertCounts(1 to "Baton")
  }

  @Test
  fun twoActionsNeedNoExtraHandoff() {
    newTestGame(playerCount = 2)
    kim.turn {
      repeat(2) { doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard") }
    }
    stan.assertCounts(1 to "Baton")
    kim.assertCounts(0 to "Pass")
  }

  @Test
  fun lastPlayerCanTakeSeveralActionsAndThenPassOutside() {
    newTestGame(playerCount = 2)
    kim.doTask("Pass")
    stan.turn {
      repeat(3) { doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard") }
    }
    stan.assertCounts(1 to "Baton", 1 to "InRound")
    stan.doTask("Pass")
    admin.assertCounts(1 to "ProductionPhase", 0 to "Baton")
  }

  @Test
  fun wrongPlayerCannotStartTurn() {
    newTestGame(playerCount = 2)
    shouldThrow<IllegalStateException> { stan.turn {} }.message shouldContain "Cannot start"
    kim.assertCounts(1 to "Baton")
  }

  @Test
  fun unfinishedPaymentIsReportedWithoutHandingOver() {
    newTestGame(playerCount = 2)
    shouldThrow<IllegalStateException> {
          kim.turn { doTasks(*standardProjectTasks("PowerPlantProject")) }
        }
        .message shouldContain "Finish"
    kim.assertCounts(1 to "Baton", 1 to "ActionScope")
    kim.doTasks("11 Pay<> FROM MC", "PROD[Energy]")
    kim.doTask("Ok")
    stan.assertCounts(1 to "Baton")
  }

  @Test
  fun cardRequiredPassCompletesItsTurn() {
    newTestGame("TurmoilExpansion, $RedAppeasement", playerCount = 2)
    kim.turn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Reds>")
    }
    stan.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    }
    kim.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$RedAppeasement"),
          "PROD[2 MC]",
          "Pass",
      )
    }
    kim.assertCounts(1 to "Pass", 0 to "InRound", 0 to "ActionScope", 0 to "Baton")
    stan.assertCounts(1 to "Baton")
  }

  @Test
  fun soloCanTakeSeveralActions() {
    newTestGame(playerCount = 1)
    kim.turn {
      repeat(3) { doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard") }
    }
    kim.assertCounts(1 to "Baton", 1 to "InRound")
    kim.doTask("Pass")
    admin.assertCounts(1 to "ProductionPhase")
  }

  @Test
  fun opponentResponseFinishesInsideTheInitiatingTurn() {
    newTestGame("$ConvoyFromEuropa", playerCount = 2)
    // Arrange the opponent's tableau; the ocean must resolve their Arctic Algae reward.
    stan.exMachina("$ArcticAlgae")
    kim.turn {
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "15 Pay<> FROM MC",
          "ProjectCard",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
      )
      giveTask(stan, "2 Plant")
      doTask("2 ProjectCard")
    }
    stan.assertCounts(1 to "Baton", 2 to "Plant")
    kim.assertCounts(0 to "ActionScope")
  }

  @Test
  fun pendingOpponentResponseCannotEndTurn() {
    newTestGame("$ConvoyFromEuropa", playerCount = 2)
    // Arrange the opponent's tableau; the ocean must resolve their Arctic Algae reward.
    stan.exMachina("$ArcticAlgae")
    shouldThrow<IllegalStateException> {
          kim.turn {
            doTasks(
                *playCardTasks("$ConvoyFromEuropa"),
                "15 Pay<> FROM MC",
                "ProjectCard",
                "OceanTile<Tharsis_2_6>",
                "TerraformRating",
                "NOW Stan { 2 Plant<Stan> }",
            )
          }
        }
        .message shouldContain "Finish"
    kim.assertCounts(1 to "Baton", 1 to "ActionScope")
    stan.doTask("2 Plant")
    kim.doTask("2 ProjectCard")
    kim.doTask("Ok")
    stan.assertCounts(1 to "Baton")
  }
}
