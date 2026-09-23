package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
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
  internal fun explicitStartCarriesBootstrapThroughSetupToCorporation() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.sneak("StartToken<Player2> FROM StartToken<Player1>")

    admin.beginOperation("WorkflowStarted")

    admin.assertCounts(
        0 to "BootstrapPhase",
        0 to "BootstrapPhaseScope",
        0 to "SetupPhase",
        0 to "SetupPhaseScope",
        1 to "CorporationPhase",
        1 to "CorporationPhaseScope",
    )
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
  }

  @Test
  internal fun rollingBackScopeRemovalRestoresItsPhaseAndContinuation() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
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
        0 to "ActionPhaseScope",
        1 to "ResearchPhase",
        1 to "ResearchPhaseScope",
    )

    game.timeline.rollBack(checkpoint)

    admin.assertCounts(
        1 to "ActionPhase",
        1 to "ActionPhaseScope",
        0 to "ProductionPhase",
        0 to "ProductionPhaseScope",
        0 to "SolarPhase",
        0 to "SolarPhaseScope",
        0 to "ResearchPhase",
        0 to "ResearchPhaseScope",
    )
    p1.assertCounts(0 to "HaveNotPassed", 1 to "Pass")
    p2.assertCounts(1 to "HaveNotPassed", 0 to "Pass")
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    admin.assertCounts(1 to "FirstActionTurn", 0 to "SecondActionTurn")
    p2.pass()
    admin.assertCounts(0 to "ActionPhase", 1 to "ResearchPhase", 2 to "Generation")
    workflow.shutdown()
  }

  @Test
  internal fun rollingBackCorporationSelectionRestoresItsTurn() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
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
            0 to "CorporationPhaseScope",
            1 to "ActionPhase",
            1 to "ActionPhaseScope",
        )

    game.timeline.rollBack(beforeFinalCorporation)

    game
        .testTfm(ADMIN)
        .assertCounts(
            1 to "CorporationPhase",
            1 to "CorporationPhaseScope",
            0 to "ActionPhase",
            0 to "ActionPhaseScope",
        )
    p1.assertCounts(0 to "CorporationCard", 1 to "UnitedNationsMarsInitiative")
    p2.assertCounts(1 to "CorporationCard", 0 to "CrediCor")
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    playCorporationWithoutStartingProjects(p2, CrediCor)
    game.testTfm(ADMIN).assertCounts(0 to "CorporationPhase", 1 to "ActionPhase")
    workflow.shutdown()
  }

  @Test
  internal fun finalGreeneryScopeCarriesTheWorkflowToEnd() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    admin.sneak("WorkflowStarted")
    admin.runOperation("FinalGreeneryPhase FROM Phase")

    admin.assertCounts(
        1 to "FinalGreeneryPhase",
        1 to "FinalGreeneryPhaseScope",
        0 to "End",
    )

    admin.runOperation("-FinalGreeneryPhaseScope")

    admin.assertCounts(
        0 to "FinalGreeneryPhase",
        0 to "FinalGreeneryPhaseScope",
        1 to "End",
    )
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
    val workflow = TfmWorkflow.Stepwise(agents)
    val p1 = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testTfm(PLAYER2).also { it.autoExecPolicy = NONE }

    workflow.setupPhase()
    p1.doTask("BeginnerCorporationCard")
    p1.doTask("NewTurn")
    p2.doTask("StandardCorporationCard")
    p2.doTask("NewTurn")

    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")

    p1.assertCounts(
        1 to "BeginnerCorporationCard",
        1 to "CorporationCard",
        0 to "ProjectCard",
        2 to "PreludeCard",
    )
    p2.assertCounts(
        0 to "BeginnerCorporationCard",
        1 to "CorporationCard",
        0 to "ProjectCard",
        2 to "PreludeCard",
    )

    workflow.corporationPhase()
    p1.startTurn()
    shouldThrow<TaskException> {
      p1.doTask("PlayCard<Class<StandardCorporationCard>, Class<CrediCor>>")
    }
    p1.doTask("PlayCard<Class<BeginnerCorporationCard>, Class<BeginnerCorporation1>>")
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
    shouldThrow<TaskException> {
      p2.doTask("PlayCard<Class<StandardCorporationCard>, Class<BeginnerCorporation2>>")
    }
    p2.doTask("PlayCard<Class<StandardCorporationCard>, Class<CrediCor>>")
    p2.pay()
    p2.doTask("57 MC")
    p2.doTask("5 BuyCard")
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
    val workflow = TfmWorkflow.Stepwise(game.testAgents())
    val p1 = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testTfm(PLAYER2).also { it.autoExecPolicy = NONE }

    workflow.setupPhase()
    listOf(p1, p2).forEach { player ->
      player.doTask("BeginnerCorporationCard")
      player.doTask("NewTurn")
    }

    workflow.corporationPhase()
    p1.startTurn()
    p1.doTask("PlayCard<Class<BeginnerCorporationCard>, Class<BeginnerCorporation1>>")
    p1.pay()
    p1.doTask("42 MC")
    p1.doTask("10 ProjectCard")

    shouldThrow<LimitsException> { p2.runOperation("BeginnerCorporation1") }
    p2.assertCounts(
        0 to "BeginnerCorporation1",
        1 to "BeginnerCorporationCard",
        0 to "ProjectCard",
    )

    p2.startTurn()
    p2.doTask("PlayCard<Class<BeginnerCorporationCard>, Class<BeginnerCorporation2>>")
    p2.pay()
    p2.doTask("42 MC")
    p2.doTask("10 ProjectCard")

    p1.assertCounts(1 to "BeginnerCorporation1", 42 to "MC", 10 to "ProjectCard")
    p2.assertCounts(1 to "BeginnerCorporation2", 42 to "MC", 10 to "ProjectCard")
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
    TfmWorkflow.Stepwise(game.testAgents()).setupPhase()
    listOf(p1, p2).forEach { player ->
      player.doTask("StandardCorporationCard")
      player.doTask("NewTurn")
    }

    shouldThrow<TaskException> { admin.doTask("-2 PreludeCard<Player1>") }
    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")

    p1.count("CorporationCard") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    p1.count("ProjectCard") shouldBe 0
    p2.count("CorporationCard") shouldBe 1
    p2.count("PreludeCard") shouldBe 2
    p2.count("ProjectCard") shouldBe 0
  }

  @Test
  internal fun researchMakesEveryPlayerQueueAvailableTogether() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val agents = game.testAgents()
    agents[PLAYER1].autoExecPolicy = NONE
    agents[PLAYER2].autoExecPolicy = NONE

    game.testTfm(PLAYER1).sneak("20 MC")
    game.testTfm(PLAYER2).sneak("20 MC")
    agents[ADMIN].beginOperation("ResearchPhase FROM Phase")

    agents[PLAYER2].doTask("2 BuyCard")
    agents[PLAYER1].doTask("BuyCard")
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
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.playCorp(InterplanetaryCinematics, 7)
    p2.playCorp(PharmacyUnion, 5)

    admin.assertCounts(
        0 to "CorporationPhaseScope",
        0 to "PreludePhase",
        0 to "PreludePhaseScope",
        1 to "ActionPhase",
        1 to "ActionPhaseScope",
    )
    p1.assertCounts(1 to "ActionPhaseStatus", 1 to "HaveNotPassed", 0 to "Pass")
    p2.assertCounts(1 to "ActionPhaseStatus", 1 to "HaveNotPassed", 0 to "Pass")

    p1.turn { sellPatents(1) }
    p2.pass()
    p1.assertCounts(1 to "HaveNotPassed", 0 to "Pass")
    p2.assertCounts(0 to "HaveNotPassed", 1 to "Pass")
    admin.assertCounts(1 to "ActionPhase", 1 to "ActionPhaseScope")
    p1.pass()

    admin.assertCounts(
        2 to "Generation",
        1 to "ResearchPhase",
        1 to "ResearchPhaseScope",
        0 to "ActionPhaseScope",
        0 to "ProductionPhaseScope",
        0 to "SolarPhaseScope",
    )

    p1.buyCards(0)
    p2.buyCards(0)

    admin.assertCounts(1 to "ActionPhase", 1 to "ActionPhaseScope", 0 to "ResearchPhaseScope")
    workflow.shutdown()
  }

  @Test
  internal fun cardThatPassesAsASecondActionRemovesThePlayerFromRotation() {
    val game = Engine.newGame(canonicalPremise(Prelude2CardPack, TurmoilExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    p1.sneak("2 ProjectCard, PartyDelegate<Reds>, PartyDelegate<Reds>")

    p1.turn {
      sellPatents(1)
      playProject(RedAppeasement, 0)
    }

    p1.assertCounts(0 to "HaveNotPassed", 1 to "Pass")
    admin.assertCounts(1 to "ActionPhase", 1 to "ActionPhaseScope")

    p2.pass()

    admin.assertCounts(0 to "ActionPhase", 0 to "ActionPhaseScope")
    workflow.shutdown()
  }

  @Test
  internal fun cardCanSupplyTheFinalPassInSoloPlay() {
    val game = Engine.newGame(canonicalPremise(Prelude2CardPack, TurmoilExpansion, players = 1))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    p1.sneak("ProjectCard, PartyDelegate<Reds>, PartyDelegate<Reds>")

    p1.playProject(RedAppeasement, 0)

    p1.assertCounts(0 to "HaveNotPassed", 1 to "Pass")
    admin.assertCounts(0 to "ActionPhase", 0 to "ActionPhaseScope")
    workflow.shutdown()
  }

  @Test
  internal fun soleRemainingPlayerDoesNotReceiveSecondActions() {
    val game = Engine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.playCorp(InterplanetaryCinematics, 7)
    p2.playCorp(PharmacyUnion, 5)

    p1.pass()
    p2.turn {
      sellPatents(1)
      sellPatents(1)
      pass()
    }

    admin.assertCounts(2 to "Generation", 1 to "ResearchPhase")
    workflow.shutdown()
  }

  @Test
  internal fun aPlayerMayPassWhileItsMandatoryFirstActionRemainsPending() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.pass()

    p1.count("Pass") shouldBe 1
    workflow.shutdown()
  }

  @Test
  internal fun rollingBackFirstActionRestoresItsTurn() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    p1.sneak("ProjectCard")
    val beforeFirstAction = game.timeline.checkpoint()

    p1.sellPatents(1)
    p1.tasks.isEmpty() shouldBe false
    game.testTfm(ADMIN).assertCounts(0 to "FirstActionTurn", 1 to "SecondActionTurn")

    game.timeline.rollBack(beforeFirstAction)

    p1.assertCounts(1 to "ProjectCard", 1 to "HaveNotPassed", 0 to "Pass")
    game.testTfm(ADMIN).assertCounts(1 to "FirstActionTurn", 0 to "SecondActionTurn")
    p1.tasks.isEmpty() shouldBe false
    p2.tasks.isEmpty() shouldBe true
    p1.pass()
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    workflow.shutdown()
  }

  @Test
  internal fun shutdownLetsTheGrantedActionFinishBeforeManualWorkflowContinues() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val agents = game.testAgents()
    val workflow = TfmWorkflow.Automatic(agents).launch()
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    workflow.shutdown()
    p1.pass()

    game.tasks.isEmpty() shouldBe true
    admin.assertCounts(1 to "ActionPhase", 0 to "ActionTurn")
    TfmWorkflow.Stepwise(agents).productionPhase()
    admin.assertCounts(0 to "ActionPhase", 1 to "ProductionPhase")
  }

  @Test
  internal fun automaticPreludePhasePlaysEveryRetainedPrelude() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    admin.sneak("StartToken<Player2> FROM StartToken<Player1>")
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.sneak("PreludeCard")

    playCorporationWithoutStartingProjects(p2, CrediCor)
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)

    admin.assertCounts(
        0 to "CorporationPhaseScope",
        1 to "PreludePhase",
        1 to "PreludePhaseScope",
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
        0 to "PreludePhaseScope",
        1 to "ActionPhase",
        1 to "ActionPhaseScope",
    )
    workflow.shutdown()
  }

  @Test
  internal fun rollingBackFinalPreludeRestoresItsPhaseAndTurn() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.playPrelude(Donation)
    p1.playPrelude(MartianIndustries)
    p2.playPrelude(DomeFarming)
    val beforeFinalPrelude = game.timeline.checkpoint()

    p2.playPrelude(Supplier)
    admin.assertCounts(
        0 to "PreludePhase",
        0 to "PreludePhaseScope",
        1 to "ActionPhase",
        1 to "ActionPhaseScope",
    )

    game.timeline.rollBack(beforeFinalPrelude)

    admin.assertCounts(
        1 to "PreludePhase",
        1 to "PreludePhaseScope",
        0 to "ActionPhase",
        0 to "ActionPhaseScope",
    )
    p1.tasks.isEmpty() shouldBe true
    p2.tasks.isEmpty() shouldBe false
    p2.assertCounts(1 to "PreludeCard", 0 to "$Supplier")
    p2.playPrelude(Supplier)
    admin.assertCounts(0 to "PreludePhase", 1 to "ActionPhase")
    workflow.shutdown()
  }

  @Test
  internal fun automaticPreludePhaseEndsWhenNoPreludeCardsWereRetained() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.sneak("-2 PreludeCard")
    p2.sneak("-2 PreludeCard")

    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    admin.assertCounts(
        0 to "PreludePhase",
        0 to "PreludePhaseScope",
        1 to "ActionPhase",
        1 to "ActionPhaseScope",
    )
    workflow.shutdown()
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
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.sneak("-PreludeCard")
    p2.sneak("-2 PreludeCard")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.playPrelude(NewPartner) { p1.playPrelude(Donation) }

    p1.count("PreludeCard") shouldBe 0
    admin.assertCounts(
        0 to "PreludePhase",
        0 to "PreludePhaseScope",
        1 to "ActionPhase",
        1 to "ActionPhaseScope",
    )

    p1.sneak("$BoardOfDirectors, Director<$BoardOfDirectors>")
    p1.cardAction1(BoardOfDirectors) { doTask("-PreludeCard") }
    admin.assertCounts(
        1 to "ActionPhase",
        0 to "PreludePhase",
        0 to "PreludeTurnContinuation",
    )
    workflow.shutdown()
  }

  @Test
  internal fun newPartnerPlayedFirstStillUsesOneOfTwoRetainedPreludeTurns() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p2.sneak("-2 PreludeCard")
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.playPrelude(NewPartner) { p1.playPrelude(Donation) }

    p1.count("PreludeCard") shouldBe 1
    admin.assertCounts(1 to "PreludePhase", 1 to "PreludePhaseScope", 0 to "ActionPhase")

    p1.playPrelude(MartianIndustries)

    p1.count("PreludeCard") shouldBe 0
    admin.assertCounts(0 to "PreludePhase", 1 to "ActionPhase")
    workflow.shutdown()
  }

  @Test
  internal fun automaticSolarWaitsForWorldGovernmentBeforeTurmoil() {
    val game = Engine.newGame(canonicalPremise(VenusNextExpansion, TurmoilExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
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
    workflow.shutdown()
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
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
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
    workflow.shutdown()
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
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)
    val lunaProduction = admin.count("ColonyProduction<Luna>")

    p1.pass()
    p2.pass()

    admin.count("ColonyProduction<Luna>") shouldBe lunaProduction + 1
    admin.assertCounts(1 to "ResearchPhase", 0 to "ColoniesSolarPhase")
    workflow.shutdown()
  }
}
