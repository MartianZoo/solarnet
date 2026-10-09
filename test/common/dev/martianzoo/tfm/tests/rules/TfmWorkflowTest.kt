package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.agenttestsupport.testTfm
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
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.BeginnerVariant
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Hellas
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
  internal fun beginnerVariantLetsEachPlayerChooseTheirStartingPath() {
    val game =
        TfmEngine.newGame(
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
    p1.assertCounts(
        0 to "BeginnerCard",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
        0 to "PlayerMode",
    )
    p2.assertCounts(
        0 to "BeginnerCard",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
        0 to "PlayerMode",
    )
    p1.doTask("BeginnerMode")
    p1.assertCounts(1 to "BeginnerMode", 0 to "BeginnerCard", 0 to "PreludeCard")
    p1.doTask("BeginnerCard")
    p1.doTask("4 PreludeCard")
    p2.doTask("NonBeginnerMode")
    p2.assertCounts(1 to "NonBeginnerMode", 0 to "CorporationCard", 0 to "PreludeCard")
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

    workflow.corporationPhase()
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
    val game = TfmEngine.newGame(canonicalPremise(BeginnerVariant, players = 2))
    val workflow = TfmWorkflow.Stepwise(game.testAgents())
    val p1 = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testTfm(PLAYER2).also { it.autoExecPolicy = NONE }

    workflow.setupPhase()
    listOf(p1, p2).forEach { player ->
      player.doTask("BeginnerMode")
      player.doTask("BeginnerCard")
    }

    workflow.corporationPhase()
    p1.startTurn()
    p1.doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation1>, Hand>")
    p1.pay()
    p1.doTask("42 MC")
    p1.doTask("10 ProjectCard")

    shouldThrow<LimitsException> { p2.runOperation("BeginnerCorporation1") }
    p2.assertCounts(
        0 to "BeginnerCorporation1",
        1 to "BeginnerCard",
        0 to "ProjectCard",
    )

    p2.startTurn()
    p2.doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation2>, Hand>")
    p2.pay()
    p2.doTask("42 MC")
    p2.doTask("10 ProjectCard")

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
  }

  @Test
  internal fun startingCardsBelongToEachPlayerAgent() {
    val game =
        TfmEngine.newGame(Canon.gamePremise(GameConfig("PreludeExpansion", "Player1", "Player2")))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    TfmWorkflow.Stepwise(game.testAgents()).setupPhase()
    p1.assertCounts(
        0 to "PlayerMode",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
    )
    p2.assertCounts(
        0 to "PlayerMode",
        0 to "CorporationCard",
        0 to "PreludeCard",
        0 to "ProjectCard",
    )
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
  internal fun startingCardCountsAreAdditiveAdjustments() {
    val game =
        TfmEngine.newGame(
            Canon.gamePremise(
                GameConfig(
                    "PreludeExpansion, 1 SelectableCorporationCount, -1 SelectablePreludeCount",
                    "Player1",
                    "Player2",
                )
            )
        )
    val players = listOf(game.testTfm(PLAYER1), game.testTfm(PLAYER2))
    players.forEach { it.autoExecPolicy = NONE }

    TfmWorkflow.Stepwise(game.testAgents()).setupPhase()
    players.forEach { player ->
      player.doTask("NonBeginnerMode")
      player.doTask("3 CorporationCard")
      player.doTask("3 PreludeCard")
      player.doTask("10 ProjectCard<Selecting>")
      player.doTask("-2 CorporationCard")
      player.doTask("-10 ProjectCard<Selecting>")
      player.doTask("-PreludeCard")
      player.assertCounts(1 to "CorporationCard", 2 to "PreludeCard")
    }
  }

  @Test
  internal fun researchMakesEveryPlayerQueueAvailableTogether() {
    val game = TfmEngine.newGame(canonicalPremise(players = 2))
    val agents = game.testAgents()
    agents[PLAYER1].autoExecPolicy = NONE
    agents[PLAYER2].autoExecPolicy = NONE

    game.testTfm(PLAYER1).sneak("20 MC")
    game.testTfm(PLAYER2).sneak("20 MC")
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
    val game = TfmEngine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(7)
    p2.keepStartingProjects(5)
    p1.playCorp(InterplanetaryCinematics)
    p2.playCorp(CrediCor)

    p1.turn { sellPatents(1) }
    p2.pass()
    p1.pass()

    admin.assertCounts(2 to "Generation", 1 to "ResearchPhase")
    workflow.shutdown()
  }

  @Test
  internal fun soleRemainingPlayerDoesNotReceiveSecondActions() {
    val game = TfmEngine.newGame(canonicalPremise(Hellas, PromoCardPack, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(7)
    p2.keepStartingProjects(5)
    p1.playCorp(InterplanetaryCinematics)
    p2.playCorp(CrediCor)

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
    val game = TfmEngine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

    p1.pass()

    p1.count("Pass") shouldBe 1
    workflow.shutdown()
  }

  @Test
  internal fun automaticPreludePhasePlaysEveryRetainedPrelude() {
    val game = TfmEngine.newGame(canonicalPremise(PreludeExpansion, players = 2))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    p1.sneak("PreludeCard")
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)

    playCorporationWithoutStartingProjects(p1, UnitedNationsMarsInitiative)
    playCorporationWithoutStartingProjects(p2, CrediCor)

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
  internal fun automaticSolarWaitsForWorldGovernmentBeforeTurmoil() {
    val game =
        TfmEngine.newGame(canonicalPremise(VenusNextExpansion, TurmoilExpansion, players = 2))
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val workflow = TfmWorkflow.Automatic(game.testAgents()).launch()
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
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
        TfmEngine.newGame(
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
    p1.keepStartingProjects(0)
    p2.keepStartingProjects(0)
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
}
