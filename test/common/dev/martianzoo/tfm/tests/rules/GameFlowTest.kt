package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.BeginnerVariant
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameFlowTest : TfmGameplayTest() {
  @Test
  internal fun startingPurchasesUseTheCorporationAlreadyInPlay() {
    for ((corporation, cost) in listOf(TerraLabsResearch to 2, Polyphemos to 10)) {
      initializeGame(
          withTestSoloSetup(
              Canon.gamePremise(GameConfig("$corporation, BeginnerVariant", "Player1"))
          )
      )
      val player = agents[PLAYER1]
      agents.taskLog.gameStarted()
      admin.beginOperation("SetupPhase FROM BootstrapPhase")
      player.doTasks("14 TerraformRating", "10 ProjectCard<Selecting>")
      player.assertCounts(
          14 to "TerraformRating",
          10 to "ProjectCard<Selecting>",
          0 to "CorporationOption",
      )
      player.selectTask("BeginnerOption OR StandardOption")
      player.narrowTask("StandardOption")
      player.doTasks("2 CorporationCard", "-CorporationCard", "-8 ProjectCard<Selecting>")
      admin.count("CorporationPhase") shouldBe 1

      player.doTasks("PlayCard<Class<CorporationCard>, Class<$corporation>>")
      player.count("$corporation") shouldBe 0
      player.count("Owed") shouldBe 0
      player.count("ProjectCard<Selecting>") shouldBe 2

      player.doTasks("$corporation FROM CorporationCard")
      player.count("$corporation") shouldBe 1
      player.count("Owed") shouldBe cost
      player.count("ProjectCard<Selecting>") shouldBe 2
      player.count("ProjectCard<Hand>") shouldBe 0

      player.completeOperation {
        if (corporation == TerraLabsResearch) {
          doTasks("14 MC", "-TerraformRating")
        } else {
          doTasks("50 MC", "5 Titanium", "PROD[5 MC]")
        }
        doTasks("$cost Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      }
      player.count("ProjectCard<Hand>") shouldBe 2
      player.count("ProjectCard<Selecting>") shouldBe 0
      player.count("Owed") shouldBe 0
      admin.count("ActionPhase") shouldBe 1
    }
  }

  @Test
  internal fun beginnerVariantLetsEachPlayerChooseTheirStartingPath() {
    initializeGame(
        Canon.gamePremise(
            GameConfig(
                "BeginnerVariant, CorporateEraExpansion, PreludeExpansion",
                "Player1",
                "Player2",
            )
        )
    )
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]

    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.assertCounts(
        0 to "BeginnerCard",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
        0 to "CorporationOption",
    )
    p2.assertCounts(
        0 to "BeginnerCard",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
        0 to "CorporationOption",
    )
    // Common setup is available before either player chooses a corporation procedure.
    listOf(p1, p2).forEach { player ->
      player.doTasks("20 TerraformRating", "10 ProjectCard<Selecting>", "4 PreludeCard")
      player.assertCounts(
          20 to "TerraformRating",
          10 to "ProjectCard<Selecting>",
          0 to "ProjectCard<Hand>",
          4 to "PreludeCard",
          0 to "CorporationOption",
      )
    }
    p1.selectTask("BeginnerOption OR StandardOption")
    p1.narrowTask("BeginnerOption")
    p1.assertCounts(1 to "BeginnerOption", 0 to "BeginnerCard")
    p1.doTask("BeginnerCard")
    p2.selectTask("BeginnerOption OR StandardOption")
    p2.narrowTask("StandardOption")
    p2.assertCounts(1 to "StandardOption", 0 to "CorporationCard")
    p2.doTask("2 CorporationCard")
    shouldThrow<TaskException> { p1.doTask("-ProjectCard<Selecting>") }
    p1.count("ProjectCard<Selecting>") shouldBe 10

    p2.doTask("-CorporationCard")
    p2.doTask("-5 ProjectCard<Selecting>")
    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")

    p1.assertCounts(
        1 to "BeginnerCard<Hand>",
        0 to "CorporationCard",
        10 to "ProjectCard<Selecting>",
        0 to "ProjectCard<Hand>",
        2 to "PreludeCard",
    )
    p2.assertCounts(
        0 to "BeginnerCard",
        1 to "CorporationCard<Hand>",
        5 to "ProjectCard<Selecting>",
        2 to "PreludeCard",
    )

    shouldThrow<TaskException> {
      p1.doTask("PlayCard<Class<CorporationCard>, Class<$CrediCor>, Hand>")
    }
    p1.doTasks(
        "PlayCard<Class<BeginnerCard>, Class<$BeginnerCorporation1>, Hand>",
        "$BeginnerCorporation1 FROM BeginnerCard<Hand>",
        "42 MC",
        "10 ProjectCard<Hand FROM Selecting>",
    )
    p1.assertCounts(
        1 to "$BeginnerCorporation1",
        42 to "MC",
        10 to "ProjectCard<Hand>",
        0 to "ProjectCard<Selecting>",
        0 to "Owed",
    )

    shouldThrow<ExpressionException> {
      p2.doTask("PlayCard<Class<CorporationCard>, Class<$BeginnerCorporation2>, Hand>")
    }
    p2.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$CrediCor>, Hand>",
        "$CrediCor FROM CorporationCard<Hand>",
        "57 MC",
        "15 Pay<> FROM MC",
        "5 ProjectCard<Hand FROM Selecting>",
    )
    p2.assertCounts(
        1 to "$CrediCor",
        42 to "MC",
        5 to "ProjectCard",
    )
  }

  @Test
  internal fun beginnerCorporationCopiesLetTwoPlayersChooseTheBeginnerPath() {
    initializeGame(canonicalPremise(BeginnerVariant, players = 2))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]

    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    listOf(p1, p2).forEach { player ->
      // Select the setup alternatives before narrowing the chosen starting path.
      player.selectTask("BeginnerOption OR StandardOption")
      player.narrowTask("BeginnerOption")
      player.doTasks("BeginnerCard", "10 ProjectCard<Selecting>", "20 TerraformRating")
    }

    p1.doTasks(
        "PlayCard<Class<BeginnerCard>, Class<$BeginnerCorporation1>, Hand>",
        "$BeginnerCorporation1 FROM BeginnerCard<Hand>",
        "42 MC",
        "10 ProjectCard<Hand FROM Selecting>",
    )

    shouldThrow<LimitsException> { p2.runOperation("$BeginnerCorporation1") }
    p2.assertCounts(
        0 to "$BeginnerCorporation1",
        1 to "BeginnerCard",
        10 to "ProjectCard<Selecting>",
        0 to "ProjectCard<Hand>",
    )

    p2.doTasks(
        "PlayCard<Class<BeginnerCard>, Class<$BeginnerCorporation2>, Hand>",
        "$BeginnerCorporation2 FROM BeginnerCard<Hand>",
        "42 MC",
        "10 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertCounts(
        1 to "BeginnerOption",
        1 to "$BeginnerCorporation1",
        42 to "MC",
        10 to "ProjectCard",
    )
    p2.assertCounts(
        1 to "BeginnerOption",
        1 to "$BeginnerCorporation2",
        42 to "MC",
        10 to "ProjectCard",
    )
  }

  @Test
  internal fun startingCardsBelongToEachPlayerAgent() {
    initializeGame(Canon.gamePremise(GameConfig("PreludeExpansion", "Player1", "Player2")))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    p1.assertCounts(
        1 to "StandardOption",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
    )
    p2.assertCounts(
        1 to "StandardOption",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
    )
    listOf(p1, p2).forEach { player ->
      player.doTasks(
          "2 CorporationCard",
          "4 PreludeCard",
          "10 ProjectCard<Selecting>",
      )
      player.assertCounts(2 to "CorporationCard<Hand>")
      player.doTasks(
          "-CorporationCard",
          "-10 ProjectCard<Selecting>",
      )
    }

    shouldThrow<TaskException> { admin.doTask("-2 PreludeCard<Player1>") }
    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")

    p1.count("CorporationCard") shouldBe 1
    p1.count("StandardOption") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    p1.count("ProjectCard") shouldBe 0
    p2.count("CorporationCard") shouldBe 1
    p2.count("StandardOption") shouldBe 1
    p2.count("PreludeCard") shouldBe 2
    p2.count("ProjectCard") shouldBe 0
    p1.doTask("20 TerraformRating")
    p2.doTask("20 TerraformRating")
    admin.count("CorporationPhase") shouldBe 1
  }

  @Test
  internal fun startingCardCountsAreAdditiveAdjustments() {
    initializeGame(
        Canon.gamePremise(
            GameConfig(
                "PreludeExpansion, 1 SelectableCorporationCount, -1 SelectablePreludeCount",
                "Player1",
                "Player2",
            )
        )
    )
    val players = listOf(agents[PLAYER1], agents[PLAYER2])

    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    players.forEach { player ->
      player.doTasks(
          "3 CorporationCard",
          "3 PreludeCard",
          "10 ProjectCard<Selecting>",
          "-2 CorporationCard",
          "-10 ProjectCard<Selecting>",
          "-PreludeCard",
      )
      player.assertCounts(1 to "CorporationCard", 2 to "PreludeCard")
      player.doTask("20 TerraformRating")
    }
    admin.count("CorporationPhase") shouldBe 1
  }

  @Test
  internal fun playerCanDeclineAnOfferedSecondAction() {
    newTestGame("-QuickStartVariant, -ColoniesExpansion, -VenusNextExpansion", playerCount = 2)
    kim.completeOperation {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }
    kim.doTask("Ok") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("20 MC") }

    admin.assertCounts(2 to "Generation", 1 to "ResearchPhase")
  }

  @Test
  internal fun soleRemainingPlayerDoesNotReceiveSecondActions() {
    newTestGame("-QuickStartVariant, -ColoniesExpansion, -VenusNextExpansion", playerCount = 2)
    kim.doTask("Pass")
    stan.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }

    admin.assertCounts(2 to "Generation", 1 to "ResearchPhase")
  }

  @Test
  internal fun solarWaitsForWorldGovernmentBeforeTurmoil() {
    newTestGame("TurmoilExpansion, -QuickStartVariant, -ColoniesExpansion", playerCount = 2)
    admin.count("Coming<$DemocraticReform>") shouldBe 1
    admin.count("Distant<$MinimalImpactPolicy>") shouldBe 1
    admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyLeader<Reds, Neutral>") shouldBe 1

    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }

    kim.count("TerraformRating") shouldBe 20
    stan.count("TerraformRating") shouldBe 20
    admin.count("SolarPhase") shouldBe 1

    kim.doTask("VenusStep! BY Admin").expect("-TerraformRating<Kim>, -TerraformRating<Stan>")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("Generation") shouldBe 2
  }

  @Test
  internal fun solarAdvancesColonyTracksAlongsideWorldGovernment() {
    newTestGame("-QuickStartVariant, Luna, Callisto, Ceres, Europa, Ganymede", playerCount = 2)
    kim.doTask("Pass")
    expectChanges("ColonyProduction<Luna>") {
      stan.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
    }

    // Neither expansion's solar work affects the other's, so the tracks need not wait.
    admin.count("SolarPhase") shouldBe 1
    kim.doTask("TemperatureStep! BY Admin").expect("0 TerraformRating")

    admin.count("ResearchPhase") shouldBe 1
  }

  @Test
  internal fun preludePhasePlaysEveryRetainedPrelude() {
    newTestGame("PreludeExpansion, -QuickStartVariant, -ColoniesExpansion", playerCount = 2)
    // An extra retained Prelude tests that the phase follows the actual hand, not a fixed count.
    kim.exMachina("PreludeCard")
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Donation>>",
          "$Donation FROM PreludeCard",
          "21 MC",
      )

      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MartianIndustries>>",
          "$MartianIndustries FROM PreludeCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
      )

      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>",
          "$PowerGeneration FROM PreludeCard",
          "PROD[3 Energy]",
      )
    }
    stan.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
          "$DomeFarming FROM PreludeCard",
          "PROD[Plant]",
          "PROD[2 MC]",
      )

      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
          "$Supplier FROM PreludeCard",
          "PROD[2 Energy]",
          "4 Steel",
      )
    }
    kim.count("PreludeCard") shouldBe 0
    stan.count("PreludeCard") shouldBe 0
    admin.count("ActionPhase") shouldBe 1
  }

  internal class Synthetic : TfmSandboxTest() {
    @Test
    internal fun researchPlayersChooseAndPayInSeparateBatches() {
      game = TfmEngine.newGame(canonicalPremise(players = 2))
      agents[PLAYER1].autoExecPolicy = NONE
      agents[PLAYER2].autoExecPolicy = NONE

      agents[PLAYER1].exMachina("20 MC")
      agents[PLAYER2].exMachina("20 MC")
      agents[ADMIN].beginOperation("ResearchPhase FROM Phase")

      // The client finishes each Player's choice and payment before starting the next purchase.
      agents[PLAYER2].doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      agents[PLAYER1].doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )

      agents[PLAYER1].count("ProjectCard") shouldBe 1
      agents[PLAYER2].count("ProjectCard") shouldBe 2
      agents[PLAYER1].count("MC") shouldBe 17
      agents[PLAYER2].count("MC") shouldBe 14
    }
  }
}
