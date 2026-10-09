package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameConfig
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine as Engine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.BeginnerVariant
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TfmWorkflowTest {
  @Test
  internal fun explicitStartWaitsForSetupBeforeCorporation() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.runOperation("StartToken<Player2> FROM StartToken<Player1>")

    admin.beginOperation("WorkflowStarted")

    admin.assertCounts(
        0 to "BootstrapPhase",
        1 to "SetupPhase",
        0 to "CorporationPhase",
    )
    p1.keepStartingProjects(0)
    admin.assertCounts(1 to "SetupPhase", 0 to "CorporationPhase")
    p2.keepStartingProjects(0)

    admin.assertCounts(
        0 to "BootstrapPhase",
        0 to "SetupPhase",
        1 to "CorporationPhase",
    )
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
  }

  @Test
  internal fun corporationHelperDoesNotChooseStartingProjectsForOtherPlayers() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.beginOperation("WorkflowStarted")

    shouldThrow<NotNowException> { p1.playCorp(CrediCor, 5) }

    admin.assertCounts(1 to "SetupPhase", 0 to "CorporationPhase")
    p1.count("ProjectCard<Selecting>") shouldBe 10
    p2.count("ProjectCard<Selecting>") shouldBe 10
  }

  @Test
  internal fun passingIsPersistentUniqueAndReversible() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("Pass.")
    p1.count("Pass") shouldBe 1
    p1.runOperation("Pass.")
    p1.count("Pass") shouldBe 1
    shouldThrow<LimitsException> { p1.runOperation("Pass") }
    p1.count("Pass") shouldBe 1

    game.timeline.rollBack(checkpoint)
    p1.count("Pass") shouldBe 0
  }

  @Test
  internal fun rollingBackLastPassRestoresItsPhaseAndTurn() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(
        p1,
        UnitedNationsMarsInitiative,
    )
    playCorporationWithoutStartingProjects(p2, CrediCor)
    p1.pass()
    val checkpoint = game.timeline.checkpoint()

    p2.pass()

    admin.assertCounts(
        0 to "ActionPhase",
        1 to "ResearchPhase",
    )

    game.timeline.rollBack(checkpoint)

    admin.assertCounts(
        1 to "ActionPhase",
        0 to "ProductionPhase",
        0 to "SolarPhase",
        0 to "ResearchPhase",
    )
    p1.assertCounts(1 to "Pass")
    p2.assertCounts(0 to "Pass")
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    admin.assertCounts(1 to "FirstActionTurn", 0 to "SecondActionTurn")
    p2.pass()
    admin.assertCounts(0 to "ActionPhase", 1 to "ResearchPhase", 2 to "Generation")
  }

  @Test
  internal fun phaseAdvancementCanRetireTheLastTurnBeforeItsOwnCleanup() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    p1.pass()

    p2.inTurn {
      doTask("Pass")
      // Exercise the other permitted cleanup order: advance while the old turn still exists.
      admin.count("ActionTurn") shouldBe 1
      admin.runOperation("-AdvancePhase<ActionPhase>")
      admin.assertCounts(0 to "ActionTurn", 1 to "ProductionPhase")
    }

    admin.assertCounts(1 to "ResearchPhase", 0 to "ActionTurn")
    p1.buyCards(0)
    p2.buyCards(0)
    admin.assertCounts(1 to "ActionPhase", 1 to "FirstActionTurn", 0 to "SecondActionTurn")
    p1.count("Pass") shouldBe 0
    p2.count("Pass") shouldBe 0
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
  }

  @Test
  internal fun rollingBackCorporationSelectionRestoresItsTurn() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
    p1.tasks.isEmpty() shouldBe false
    p2.tasks.isEmpty() shouldBe true

    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    val beforeFinalCorporation = game.timeline.checkpoint()

    playCorporationWithoutStartingProjects(p2, CrediCor)
    game
        .testTfm(ADMIN)
        .assertCounts(
            0 to "CorporationPhase",
            1 to "ActionPhase",
        )

    game.timeline.rollBack(beforeFinalCorporation)

    game
        .testTfm(ADMIN)
        .assertCounts(
            1 to "CorporationPhase",
            0 to "ActionPhase",
        )
    p1.assertCounts(0 to "CorporationCard", 1 to "UnitedNationsMarsInitiative")
    p2.assertCounts(1 to "CorporationCard", 0 to "CrediCor")
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    playCorporationWithoutStartingProjects(p2, CrediCor)
    game.testTfm(ADMIN).assertCounts(0 to "CorporationPhase", 1 to "ActionPhase")
  }

  @Test
  internal fun beginnerVariantLetsEachPlayerChooseTheirStartingPath() {
    val game =
        Engine.newGame(
            Canon.gamePremise(
                GameConfig(
                    "BeginnerVariant, CorporateEraExpansion, PreludeExpansion",
                    "Player1",
                    "Player2",
                )
            )
        )
    val agents = game.testAgents()
    val admin = agents[ADMIN]
    val p1 = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testTfm(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("SetupPhase FROM Phase")
    p1.doTask("BeginnerMode")
    p1.doTask("BeginnerCard")
    p1.doTask("4 PreludeCard")
    p2.doTask("NonBeginnerMode")
    p2.doTask("2 CorporationCard")
    p2.doTask("4 PreludeCard")
    p2.doTask("10 ProjectCard<Selecting>")
    p2.doTask("-CorporationCard")
    p2.doTask("-5 ProjectCard<Selecting>")
    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")

    p1.assertCounts(
        1 to "BeginnerCard<Hand>",
        0 to "CorporationCard",
        0 to "ProjectCard<Selecting>",
        2 to "PreludeCard",
    )
    p2.assertCounts(
        0 to "BeginnerCard",
        1 to "CorporationCard<Hand>",
        5 to "ProjectCard<Selecting>",
        2 to "PreludeCard",
    )

    admin.runOperation("CorporationPhase FROM Phase")
    p1.startTurn()
    shouldThrow<TaskException> {
      p1.doTask("PlayCard<Class<CorporationCard>, Class<CrediCor>, Hand>")
    }
    p1.doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation1>, Hand>")
    p1.pay()
    p1.doTask("42 MC")
    p1.doTask("10 ProjectCard")
    p1.assertCounts(
        1 to "BeginnerCorporation1",
        42 to "MC",
        10 to "ProjectCard",
        0 to "Owed",
    )

    p2.startTurn()
    shouldThrow<ExpressionException> {
      p2.doTask("PlayCard<Class<CorporationCard>, Class<BeginnerCorporation2>, Hand>")
    }
    p2.doTask("PlayCard<Class<CorporationCard>, Class<CrediCor>, Hand>")
    p2.pay()
    p2.doTask("57 MC")
    p2.doTask("BuySelectedCards")
    p2.pay(15)
    p2.assertCounts(
        1 to "CrediCor",
        42 to "MC",
        5 to "ProjectCard",
    )
  }

  @Test
  internal fun beginnerCorporationCopiesLetTwoPlayersChooseTheBeginnerPath() {
    val game = Engine.newGame(canonicalPremise(BeginnerVariant, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testTfm(PLAYER2).also { it.autoExecPolicy = NONE }

    admin.beginOperation("WorkflowStarted")
    listOf(p1, p2).forEach { player ->
      player.doTask("BeginnerMode")
      player.doTask("BeginnerCard")
    }

    p1.tasks.isEmpty() shouldBe false
    p2.tasks.isEmpty() shouldBe true
    p1.doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation1>, Hand>")
    p1.pay()
    p1.doTask("42 MC")
    p1.doTask("10 ProjectCard")
    p1.doTask("BuySelectedCards")

    shouldThrow<LimitsException> { p2.runOperation("BeginnerCorporation1") }
    p2.assertCounts(
        0 to "BeginnerCorporation1",
        1 to "BeginnerCard",
        0 to "ProjectCard",
    )
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false

    p2.doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation2>, Hand>")
    p2.pay()
    p2.doTask("42 MC")
    p2.doTask("10 ProjectCard")
    p2.doTask("BuySelectedCards")

    p1.assertCounts(
        1 to "BeginnerMode",
        1 to "BeginnerCorporation1",
        42 to "MC",
        10 to "ProjectCard",
    )
    p2.assertCounts(
        1 to "BeginnerMode",
        1 to "BeginnerCorporation2",
        42 to "MC",
        10 to "ProjectCard",
    )
    admin.assertCounts(0 to "CorporationPhase", 1 to "ActionPhase")
  }

  @Test
  internal fun startingCardsBelongToEachPlayerAgent() {
    val game =
        Engine.newGame(Canon.gamePremise(GameConfig("PreludeExpansion", "Player1", "Player2")))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    game.testAgents()[ADMIN].beginOperation("SetupPhase FROM Phase")
    listOf(p1, p2).forEach { player ->
      player.doTask("NonBeginnerMode")
      player.doTask("2 CorporationCard")
      player.doTask("4 PreludeCard")
      player.doTask("10 ProjectCard<Selecting>")
      player.assertCounts(2 to "CorporationCard<Hand>")
      player.doTask("-CorporationCard")
      player.doTask("-10 ProjectCard<Selecting>")
    }

    shouldThrow<TaskException> { admin.doTask("-2 PreludeCard<Player1>") }
    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")

    p1.count("CorporationCard") shouldBe 1
    p1.count("NonBeginnerMode") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    p1.count("ProjectCard") shouldBe 0
    p2.count("CorporationCard") shouldBe 1
    p2.count("NonBeginnerMode") shouldBe 1
    p2.count("PreludeCard") shouldBe 2
    p2.count("ProjectCard") shouldBe 0
  }

  @Test
  internal fun researchMakesEveryPlayerQueueAvailableTogether() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val agents = game.testAgents()
    agents[PLAYER1].autoExecPolicy = NONE
    agents[PLAYER2].autoExecPolicy = NONE

    game.testTfm(PLAYER1).runOperation("20 MC")
    game.testTfm(PLAYER2).runOperation("20 MC")
    agents[ADMIN].beginOperation("ResearchPhase FROM Phase")

    agents[PLAYER2].doTask("4 ProjectCard<Selecting>")
    agents[PLAYER2].doTask("-2 ProjectCard<Selecting>")
    agents[PLAYER2].doTask("BuySelectedCards")
    agents[PLAYER1].doTask("4 ProjectCard<Selecting>")
    agents[PLAYER1].doTask("-3 ProjectCard<Selecting>")
    agents[PLAYER1].doTask("BuySelectedCards")
    game.testTfm(PLAYER2).pay(6)
    game.testTfm(PLAYER1).pay(3)

    agents[PLAYER1].count("ProjectCard") shouldBe 1
    agents[PLAYER2].count("ProjectCard") shouldBe 2
  }

  @Test
  internal fun turnDeclinesAnUnusedSecondAction() {
    val game = Engine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    p1.keepStartingProjects(7)
    p2.keepStartingProjects(5)
    p1.playCorp(InterplanetaryCinematics)
    p2.playCorp(PharmacyUnion) {
      doTask("Disease<$PharmacyUnion>")
      doTask("Disease<$PharmacyUnion>")
    }

    admin.assertCounts(
        0 to "PreludePhase",
        1 to "ActionPhase",
    )
    p1.assertCounts(0 to "Pass")
    p2.assertCounts(0 to "Pass")

    p1.turn { sellPatents(1) }
    p2.pass()
    p1.assertCounts(0 to "Pass")
    p2.assertCounts(1 to "Pass")
    admin.assertCounts(1 to "ActionPhase")
    p1.pass()

    admin.assertCounts(
        2 to "Generation",
        1 to "ResearchPhase",
        0 to "ActionPhase",
        0 to "ActionTurn",
    )
    p1.count("Pass") shouldBe 1
    p2.count("Pass") shouldBe 1

    p1.buyCards(0)
    p2.buyCards(0)

    admin.assertCounts(1 to "ActionPhase")
    p1.count("Pass") shouldBe 0
    p2.count("Pass") shouldBe 0
  }

  @Test
  internal fun cardThatPassesAsASecondActionRemovesThePlayerFromRotation() {
    val game = Engine.newGame(canonicalPremise(Prelude2CardPack, TurmoilExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.runOperation("2 ProjectCard, PartyDelegate<Reds>, PartyDelegate<Reds>")
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.turn {
      sellPatents(1)
      playProject(RedAppeasement, 0)
    }

    p1.assertCounts(1 to "Pass")
    admin.assertCounts(1 to "ActionPhase")

    p2.pass()

    admin.assertCounts(0 to "ActionPhase")
  }

  @Test
  internal fun cardCanSupplyTheFinalPassInSoloPlay() {
    val game = Engine.newGame(canonicalPremise(Prelude2CardPack, TurmoilExpansion, players = 1))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("ProjectCard, PartyDelegate<Reds>, PartyDelegate<Reds>")
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)

    p1.playProject(RedAppeasement, 0)

    p1.assertCounts(1 to "Pass")
    admin.assertCounts(0 to "ActionPhase")
  }

  @Test
  internal fun soleRemainingPlayerDoesNotReceiveSecondActions() {
    val game = Engine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    p1.keepStartingProjects(7)
    p2.keepStartingProjects(5)
    p1.playCorp(InterplanetaryCinematics)
    p2.playCorp(PharmacyUnion) {
      doTask("Disease<$PharmacyUnion>")
      doTask("Disease<$PharmacyUnion>")
    }

    p1.pass()
    p2.turn {
      sellPatents(1)
      sellPatents(1)
      pass()
    }

    admin.assertCounts(2 to "Generation", 1 to "ResearchPhase")
  }

  @Test
  internal fun aPlayerMayPassWhileItsMandatoryFirstActionRemainsPending() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.pass()

    p1.count("Pass") shouldBe 1
  }

  @Test
  internal fun rollingBackFirstActionRestoresItsTurn() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.runOperation("ProjectCard")
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    val beforeFirstAction = game.timeline.checkpoint()

    p1.sellPatents(1)
    p1.tasks.isEmpty() shouldBe false
    game.testTfm(ADMIN).assertCounts(0 to "FirstActionTurn", 1 to "SecondActionTurn")

    game.timeline.rollBack(beforeFirstAction)

    p1.assertCounts(1 to "ProjectCard", 0 to "Pass")
    game.testTfm(ADMIN).assertCounts(1 to "FirstActionTurn", 0 to "SecondActionTurn")
    p1.tasks.isEmpty() shouldBe false
    p2.tasks.isEmpty() shouldBe true
    p1.pass()
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
  }

  @Test
  internal fun stoppingAfterTheGrantedActionAllowsManualPhaseChanges() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val agents = game.testAgents()
    agents[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.inTurn {
      doTask("Pass")
      admin.runOperation("-WorkflowStarted")
    }

    game.tasks.isEmpty() shouldBe true
    admin.assertCounts(1 to "ActionPhase", 0 to "ActionTurn")
    agents[ADMIN].runOperation("ProductionPhase FROM Phase")
    admin.assertCounts(0 to "ActionPhase", 1 to "ProductionPhase")
  }

  @Test
  internal fun automaticPreludePhasePlaysEveryRetainedPrelude() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.runOperation("StartToken<Player2> FROM StartToken<Player1>")
    p1.runOperation("PreludeCard")
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")

    playCorporationWithoutStartingProjects(p2, CrediCor)
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)

    admin.assertCounts(
        1 to "PreludePhase",
        0 to "ActionPhase",
    )
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false

    p2.turn {
      playPrelude(DomeFarming)
      playPrelude(Supplier)
    }
    p1.turn {
      playPrelude(Donation)
      playPrelude(MartianIndustries)
      playPrelude(PowerGeneration)
    }

    p1.count("PreludeCard") shouldBe 0
    p2.count("PreludeCard") shouldBe 0
    admin.assertCounts(
        0 to "PreludePhase",
        1 to "ActionPhase",
    )
  }

  @Test
  internal fun rollingBackFinalPreludeRestoresItsPhaseAndTurn() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.playPrelude(Donation)
    p1.playPrelude(MartianIndustries)
    p2.playPrelude(DomeFarming)
    val beforeFinalPrelude = game.timeline.checkpoint()

    p2.playPrelude(Supplier)
    admin.assertCounts(
        0 to "PreludePhase",
        1 to "ActionPhase",
    )

    game.timeline.rollBack(beforeFinalPrelude)

    admin.assertCounts(
        1 to "PreludePhase",
        0 to "ActionPhase",
    )
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    p2.assertCounts(1 to "PreludeCard", 0 to "$Supplier")
    p2.playPrelude(Supplier)
    admin.assertCounts(0 to "PreludePhase", 1 to "ActionPhase")
  }

  @Test
  internal fun automaticPreludePhaseEndsWhenNoPreludeCardsWereRetained() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    p1.sneak("-2 PreludeCard")
    p2.sneak("-2 PreludeCard")

    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    admin.assertCounts(
        0 to "PreludePhase",
        1 to "ActionPhase",
    )
  }

  @Test
  internal fun preludeCompletionWaitsForCardsGrantedByTheLastPrelude() {
    val game =
        Engine.newGame(
            canonicalPremise(
                PreludeExpansion,
                Prelude2CardPack,
                PromoCardPack,
                players = 2,
            )
        )
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    p1.sneak("-PreludeCard")
    p2.sneak("-2 PreludeCard")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.playPrelude(NewPartner) { p1.playPrelude(Donation) }

    p1.count("PreludeCard") shouldBe 0
    admin.assertCounts(
        0 to "PreludePhase",
        1 to "ActionPhase",
    )

    p1.sneak("$BoardOfDirectors, Director<$BoardOfDirectors>")
    p1.cardAction1(BoardOfDirectors) { doTask("-PreludeCard") }
    admin.assertCounts(
        1 to "ActionPhase",
        0 to "PreludePhase",
        0 to "PreludeTurnContinuation",
    )
  }

  @Test
  internal fun newPartnerPlayedFirstStillUsesOneOfTwoRetainedPreludeTurns() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    p2.sneak("-2 PreludeCard")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.playPrelude(NewPartner) { p1.playPrelude(Donation) }

    p1.count("PreludeCard") shouldBe 1
    admin.assertCounts(1 to "PreludePhase", 0 to "ActionPhase")

    p1.playPrelude(MartianIndustries)

    p1.count("PreludeCard") shouldBe 0
    admin.assertCounts(0 to "PreludePhase", 1 to "ActionPhase")
  }

  @Test
  internal fun automaticSolarWaitsForWorldGovernmentBeforeTurmoil() {
    val game = Engine.newGame(canonicalPremise(VenusNextExpansion, TurmoilExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.pass()
    p2.pass()

    p1.count("TerraformRating") shouldBe 20
    p2.count("TerraformRating") shouldBe 20
    admin.count("VenusSolarPhase") shouldBe 1
    admin.count("TurmoilSolarOperation") shouldBe 0

    p1.doTask("VenusStep! BY Admin")

    p1.count("TerraformRating") shouldBe 19
    p2.count("TerraformRating") shouldBe 19
    admin.count("TurmoilSolarPhase") shouldBe 1
  }

  @Test
  internal fun automaticSolarWaitsForWorldGovernmentBeforeColonies() {
    val game =
        Engine.newGame(
            canonicalPremise(
                VenusNextExpansion,
                ColoniesExpansion,
                players = 2,
                colonyTiles = testColonyTiles(2, "Luna"),
            )
        )
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    val lunaProduction = admin.count("ColonyProduction<Luna>")

    p1.pass()
    p2.pass()

    admin.count("VenusSolarPhase") shouldBe 1
    admin.count("ColoniesSolarPhase") shouldBe 0
    admin.count("ColonyProduction<Luna>") shouldBe lunaProduction

    p1.doTask("TemperatureStep! BY Admin")

    admin.count("ColonyProduction<Luna>") shouldBe lunaProduction + 1
  }

  @Test
  internal fun automaticSolarRunsColoniesWithoutVenus() {
    val game =
        Engine.newGame(
            canonicalPremise(
                ColoniesExpansion,
                players = 2,
                colonyTiles = testColonyTiles(2, "Luna"),
            )
        )
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    game.testAgents()[ADMIN].beginOperation("WorkflowStarted")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    val lunaProduction = admin.count("ColonyProduction<Luna>")

    p1.pass()
    p2.pass()

    admin.count("ColonyProduction<Luna>") shouldBe lunaProduction + 1
    admin.assertCounts(1 to "ResearchPhase", 0 to "ColoniesSolarPhase")
  }
}
