package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
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

    admin.beginOperation("WorkflowStarted")

    admin.assertCounts(
        0 to "BootstrapPhase",
        0 to "BootstrapPhaseScope",
        1 to "SetupPhase",
        1 to "SetupPhaseScope",
        0 to "CorporationPhase",
    )

    game.retainStartingProjects(0, 0)

    admin.assertCounts(
        0 to "SetupPhase",
        0 to "SetupPhaseScope",
        1 to "CorporationPhase",
    )
  }

  @Test
  internal fun rollingBackScopeRemovalRestoresItsPhaseAndContinuation() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val admin = game.testTfm(ADMIN)
    admin.beginOperation("WorkflowStarted")
    game.retainStartingProjects(0, 0)
    admin.runOperation("ActionPhase FROM Phase")
    val checkpoint = game.timeline.checkpoint()

    admin.beginOperation("-ActionPhaseScope")

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
    game.tasks.isEmpty() shouldBe true
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
  internal fun startingCardDiscardsBelongToEachPlayerAgent() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    TfmWorkflow.Stepwise(game.testAgents()).setupPhase()
    listOf(p1, p2).forEach { player ->
      player.doTask("2 CorporationCard")
      player.doTask("10 ProjectCard")
      player.doTask("NewTurn")
    }

    shouldThrow<TaskException> { admin.doTask("-CorporationCard<Player1>") }
    p1.doTask("-CorporationCard")
    p2.doTask("-CorporationCard")
    shouldThrow<TaskException> { admin.doTask("-2 PreludeCard<Player1>") }
    p1.doTask("-2 PreludeCard")
    p2.doTask("-2 PreludeCard")
    p1.doTask("10 ProjectCard<Selecting FROM Hand>")
    p2.doTask("10 ProjectCard<Selecting FROM Hand>")
    shouldThrow<TaskException> { admin.doTask("-3 ProjectCard<Player1, Selecting>") }
    p1.doTask("-3 ProjectCard<Selecting>")
    p1.doTask("7 ProjectCard<Hand FROM Selecting>")
    p2.doTask("-5 ProjectCard<Selecting>")
    p2.doTask("5 ProjectCard<Hand FROM Selecting>")

    p1.count("CorporationCard") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    p1.count("ProjectCard<Hand>") shouldBe 7
    p2.count("CorporationCard") shouldBe 1
    p2.count("PreludeCard") shouldBe 2
    p2.count("ProjectCard<Hand>") shouldBe 5
  }

  @Test
  internal fun researchMakesEveryPlayerQueueAvailableTogether() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val agents = game.testAgents()
    agents[PLAYER1].autoExecPolicy = NONE
    agents[PLAYER2].autoExecPolicy = NONE

    agents[ADMIN].beginOperation("ResearchPhase FROM Phase")

    agents[PLAYER2].doTask("4 ProjectCard<Selecting>")
    agents[PLAYER1].doTask("4 ProjectCard<Selecting>")

    agents[PLAYER1].count("ProjectCard<Selecting>") shouldBe 4
    agents[PLAYER2].count("ProjectCard<Selecting>") shouldBe 4
  }

  @Test
  internal fun turnDeclinesAnUnusedSecondAction() {
    val game = Engine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    game.retainStartingProjects(7, 5)

    p1.playCorp(InterplanetaryCinematics, 7)
    p2.playCorp(PharmacyUnion, 5)

    admin.assertCounts(1 to "ActionPhase", 1 to "ActionPhaseScope")

    p1.turn { sellPatents(1) }
    p2.pass()
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
  internal fun soleRemainingPlayerDoesNotReceiveSecondActions() {
    val game = Engine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    game.retainStartingProjects(7, 5)

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
    game.retainStartingProjects(0, 0)
    p1.playCorp(UnitedNationsMarsInitiative)
    p2.playCorp(CrediCor)

    p1.pass()

    p1.count("Pass") shouldBe 1
    workflow.shutdown()
  }

  @Test
  internal fun automaticPreludePhasePlaysEveryRetainedPrelude() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    game.retainStartingProjects(0, 0)
    p1.sneak("PreludeCard")

    p1.playCorp(UnitedNationsMarsInitiative)
    p2.playCorp(CrediCor)

    p1.turn {
      playPrelude(Donation)
      playPrelude(MartianIndustries)
      playPrelude(PowerGeneration)
    }
    p2.turn {
      playPrelude(DomeFarming)
      playPrelude(Supplier)
    }

    p1.count("PreludeCard") shouldBe 0
    p2.count("PreludeCard") shouldBe 0
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
    game.retainStartingProjects(0, 0)
    p1.playCorp(UnitedNationsMarsInitiative)
    p2.playCorp(CrediCor)
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
    game.retainStartingProjects(0, 0)
    p1.playCorp(UnitedNationsMarsInitiative)
    p2.playCorp(CrediCor)
    val lunaProduction = admin.count("ColonyProduction<Luna>")

    p1.pass()
    p2.pass()

    admin.count("ColonyProduction<Luna>") shouldBe lunaProduction + 1
    admin.assertCounts(1 to "ResearchPhase", 0 to "ColoniesSolarPhase")
    workflow.shutdown()
  }
}
