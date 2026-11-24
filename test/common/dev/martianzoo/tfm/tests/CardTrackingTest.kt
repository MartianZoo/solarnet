package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.InputRequiredException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.engine.CardTracking.NamesRequiredException
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardTrackingTest : TfmSandboxTest() {
  private fun newTrackedGame(
      expansions: String = "PreludeExpansion",
      requireSelectingNames: Boolean = false,
      namesFor: ((NamesRequiredException) -> List<ClassName>)? = null,
  ): Pair<CardTracking, Player> {
    val config = GameConfig(expansions, "Player1")
    game =
        TfmEngine.newGame(
            withTestTurmoilDeck(withTestSoloSetup(canonicalCatalog(config).gamePremise(config)))
        )
    val player = game.actors.filterIsInstance<Player>().single()
    return CardTracking.start(agents, requireSelectingNames, namesFor) to player
  }

  @Test
  internal fun requestedNamesSurviveAnEnclosingRollbackAndAreRevalidated() {
    var requests = 0
    val (tracking, player) =
        newTrackedGame(
            namesFor = {
              requests++
              it.count shouldBe 1
              listOf(cn("Mine"))
            }
        )
    val agent = agents[player]
    val before = game.events.nextOrdinal
    shouldThrow<IllegalStateException> {
      game.timeline.atomic {
        agent.runOperation("ProjectCard")
        tracking.hand(player) shouldBe setOf(cn("Mine"))
        error("Later work failed")
      }
    }
    game.events.nextOrdinal shouldBe before
    agent.count("ProjectCard") shouldBe 0
    tracking.hand(player) shouldBe emptySet()
    agent.runOperation("ProjectCard")
    tracking.hand(player) shouldBe setOf(cn("Mine"))
    requests shouldBe 1
    tracking.requireComplete()
  }

  @Test
  internal fun requestedNamesCannotDuplicateACardAlreadyHeld() {
    val (tracking, player) = newTrackedGame(namesFor = { listOf(cn("Mine")) })
    val agent = agents[player]
    agent.runOperation("ProjectCard")
    val before = game.events.nextOrdinal
    shouldThrow<IllegalStateException> { agent.runOperation("ProjectCard") }
    game.events.nextOrdinal shouldBe before
    agent.count("ProjectCard") shouldBe 1
    tracking.hand(player) shouldBe setOf(cn("Mine"))
    tracking.replacePendingArrivals(player, cn("TitaniumMine"))
    agent.runOperation("ProjectCard")
    tracking.hand(player) shouldBe setOf(cn("Mine"), cn("TitaniumMine"))
    tracking.requireComplete()
  }

  @Test
  internal fun aStoredCardCanBePlayedDirectlyFromItsTrackedStagingLocation() {
    val (tracking, player) = newTrackedGame("$SelfReplicatingRobots")
    val agent = agents[player].also { it.autoExecPolicy = NONE }
    agents.runSystem("Generation")
    agent.runOperation("$SelfReplicatingRobots, 2 MC")
    tracking.expectArrivals(player, Mine)
    agent.runOperation("ProjectCard")

    agent.runOperation("UseAction<UseActionOnCardAction, Action1>") {
      doTask("UseAction<$SelfReplicatingRobots, Action1>")

      doTasks(
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    tracking.hand(player) shouldBe emptySet()
    agent.count("ProjectCard<StagedProject<Class<$Mine>>>") shouldBe 1
    agent.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 2

    agent.runOperation("UseAction<PlayCardAction, Action1>") {
      doTask("PlayCard<Class<ProjectCard>, Class<$Mine>, StagedProject<Class<$Mine>>>")

      doTasks(
          "2 Pay<> FROM MC",
          "PROD[Steel]",
      )
    }
    tracking.hand(player) shouldBe emptySet()
    agent.count("$Mine") shouldBe 1
    agent.count("RobotUnit") shouldBe 0
    agent.count("MC") shouldBe 0
    val afterPlay = game.events.nextOrdinal
    tracking.expectArrivals(player, Mine)
    shouldThrow<IllegalStateException> { agent.runOperation("ProjectCard") }
    game.events.nextOrdinal shouldBe afterPlay
    tracking.replacePendingArrivals(player)
    tracking.requireComplete()
  }

  @Test
  internal fun stagingIdentifiesWhichCardLeftTheTrackedHand() {
    val (tracking, player) = newTrackedGame("$SelfReplicatingRobots")
    val agent = agents[player].also { it.autoExecPolicy = NONE }
    agents.runSystem("Generation")
    agent.runOperation("$SelfReplicatingRobots, 2 MC")
    tracking.expectArrivals(player, Mine, TitaniumMine)
    agent.runOperation("ProjectCard")
    agent.runOperation("ProjectCard")

    agent.runOperation("UseAction<UseActionOnCardAction, Action1>") {
      doTask("UseAction<$SelfReplicatingRobots, Action1>")

      doTasks(
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    tracking.hand(player) shouldBe setOf(TitaniumMine)
    val afterStaging = game.events.nextOrdinal
    shouldThrow<IllegalStateException> {
      agent.runOperation("UseAction<PlayCardAction, Action1>") {
        doTasks(
            "PlayCard<Class<ProjectCard>, Class<$Mine>, Hand>",
            "2 Pay<> FROM MC",
            "PROD[Steel]",
        )
      }
    }
    game.events.nextOrdinal shouldBe afterStaging
    agent.count("ProjectCard<StagedProject<Class<$Mine>>>") shouldBe 1
    tracking.requireComplete()
  }

  @Test
  internal fun aNamedCorporationPlayMustComeFromTheCorporationHand() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, Ecoline, back = cn("CorporationCard"))
    val agent = agents[player]
    agent.runOperation("CorporationCard")
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agent.runOperation("$CrediCor FROM CorporationCard") { doTask("57 MC") }
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(Ecoline)
  }

  @Test
  internal fun astraMechanicaReturnsLawSuitFromTheAttackersEventPileToHand() {
    val config =
        GameConfig(
            "PreludeExpansion, $LawSuit, $AstraMechanica, $MineralDeposit",
            "Player1",
            "Player2",
        )
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    val (holder, attacker) = game.actors.filterIsInstance<Player>()
    val tracking = CardTracking.start(agents)
    agents.runSystem("Generation")
    agents[attacker].runOperation("3 MC")
    agents[holder].runOperation("ProductionWasAttacked<$holder, Class<Energy>, $attacker>")
    tracking.expectArrivals(holder, LawSuit)
    agents[holder].runOperation("ProjectCard")
    val before = game.events.nextOrdinal

    agents[holder].runOperation("$LawSuit FROM ProjectCard") {
      // The payment's continuation transfers the played event through Admin.
      doTask("3 MC<$holder FROM $attacker>")
    }

    game.events.changesSince(before).mapNotNull { it.notes } shouldBe listOf("Cards: $LawSuit")
    agents[attacker].count("PlayedEvent<$attacker, Class<$LawSuit>>") shouldBe 1
    tracking.expectArrivals(attacker, MineralDeposit, AstraMechanica)
    agents[attacker].runOperation("2 ProjectCard")
    agents[attacker].runOperation("$MineralDeposit FROM ProjectCard") { doTask("5 Steel") }
    val beforeAstra = game.events.nextOrdinal

    agents[attacker].runOperation("$AstraMechanica FROM ProjectCard") {
      doTasks(
          "ProjectCard FROM PlayedEvent<Class<$LawSuit>>",
          "ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>",
      )
    }

    tracking.hand(attacker) shouldBe setOf(LawSuit, MineralDeposit)
    tracking.hand(holder) shouldBe emptySet()
    game.events.changesSince(beforeAstra).mapNotNull { it.notes } shouldBe
        listOf("Cards: $AstraMechanica", "Cards: $LawSuit", "Cards: $MineralDeposit")
    tracking.requireComplete()
  }

  @Test
  internal fun astraMechanicaCannotTakePharmacyUnionInATrackedGame() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, $AstraMechanica, $PharmacyUnion, $MineralDeposit, $BribedCommittee"
        )
    val agent = agents[player]
    tracking.expectArrivals(
        player,
        MineralDeposit,
        BribedCommittee,
        AstraMechanica,
    )
    agent.runOperation("3 ProjectCard")
    agent.runOperation("$MineralDeposit FROM ProjectCard") { doTask("5 Steel") }
    agent.runOperation("$BribedCommittee FROM ProjectCard") { doTask("2 TerraformRating") }
    // Pharmacy Union can become a PlayedEvent, but it is a corporation rather than an EventCard.
    agent.exMachina("PlayedEvent<Class<$PharmacyUnion>>")
    val before = game.events.nextOrdinal

    shouldThrow<TaskException> {
      agent.runOperation("$AstraMechanica FROM ProjectCard") {
        doTask("ProjectCard FROM PlayedEvent<Class<$PharmacyUnion>>")
      }
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(AstraMechanica)
    agent.runOperation("$AstraMechanica FROM ProjectCard") {
      doTasks(
          "ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>",
          "ProjectCard FROM PlayedEvent<Class<$BribedCommittee>>",
      )
    }
    tracking.hand(player) shouldBe setOf(MineralDeposit, BribedCommittee)
    agent.count("PlayedEvent<Class<$PharmacyUnion>>") shouldBe 1
    tracking.requireComplete()
  }

  @Test
  internal fun discardedCardMayEnterTheHandAgain() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    val card = AdaptedLichen
    tracking.expectArrivals(player, card)
    agent.runOperation("ProjectCard")
    tracking.expectDepartures(player, card)
    agent.runOperation("-ProjectCard")
    tracking.expectArrivals(player, card)
    agent.runOperation("ProjectCard")

    tracking.hand(player) shouldBe setOf(card)
    tracking.requireComplete()
  }

  @Test
  internal fun aNamedPlayMustBeInThatPlayersHand() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, AdaptedLichen)
    agent.runOperation("ProjectCard")
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agent.runOperation("$AcquiredCompany FROM ProjectCard") { doTask("PROD[3 MC]") }
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(AdaptedLichen)
  }

  @Test
  internal fun aResourceExchangeUsesTheNamedHandDeparture() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    val card = AdaptedLichen
    tracking.expectArrivals(player, card)
    agent.runOperation("ProjectCard")
    tracking.expectDepartures(player, card)

    agent.runOperation("MC FROM ProjectCard")

    tracking.hand(player) shouldBe emptySet()
    tracking.requireComplete()
  }

  @Test
  internal fun aFilteredArrivalMustHaveThePrintedTag() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, AcquiredCompany)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agents[player].runOperation("SearchForCard<TagFilter<Class<PlantTag>>>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aFilteredBatchChecksEveryCard() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, AdaptedLichen, AcquiredCompany)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agents[player].runOperation("2 SearchForCard<TagFilter<Class<PlantTag>>>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aTaglessFilterUsesPrintedTags() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, MicroMills, ResearchCoordination)
    agents[player].runOperation("2 SearchForCard<NoTagsFilter>")

    tracking.hand(player) shouldBe setOf(MicroMills, ResearchCoordination)
    tracking.requireComplete()
  }

  @Test
  internal fun anEventIconCountsAsAPrintedTag() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, MiningExpedition)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agents[player].runOperation("SearchForCard<NoTagsFilter>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aRequirementFilterAcceptsBothTagAndGlobalRequirements() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, MassConverter, Algae)
    agents[player].runOperation("2 SearchForCard<RequirementFilter>")

    tracking.hand(player) shouldBe setOf(MassConverter, Algae)
    tracking.requireComplete()
  }

  @Test
  internal fun aRequirementFilterRejectsCardsWithoutRequirements() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, AcquiredCompany)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agents[player].runOperation("SearchForCard<RequirementFilter>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aReferenceFilterReadsTheCardDeclaration() {
    val (tracking, player) = newTrackedGame("PreludeExpansion, VenusNextExpansion")
    tracking.expectArrivals(player, Dirigibles)
    agents[player].runOperation("SearchForCard<ReferenceFilter<Class<Floater>>>")

    tracking.hand(player) shouldBe setOf(Dirigibles)
    tracking.requireComplete()
  }

  @Test
  internal fun aReferenceFilterRejectsAnUnrelatedCard() {
    val (tracking, player) = newTrackedGame("PreludeExpansion, VenusNextExpansion")
    tracking.expectArrivals(player, VenusGovernor)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agents[player].runOperation("SearchForCard<ReferenceFilter<Class<Floater>>>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aReferenceFilterReadsThePrintedRequirement() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, VenusNextExpansion, prelude2:cards, promos:cards, TurmoilExpansion"
        )
    tracking.expectArrivals(player, SupportedResearch)
    agents[player].runOperation("SearchForCard<ReferenceFilter<Class<Ruling>>>")

    tracking.hand(player) shouldBe setOf(SupportedResearch)
    tracking.requireComplete()
  }

  @Test
  internal fun aPartyRequirementSearchRejectsACardThatOnlyMovesDelegates() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, VenusNextExpansion, prelude2:cards, promos:cards, TurmoilExpansion"
        )
    tracking.expectArrivals(player, BannedDelegate)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agents[player].runOperation("SearchForCard<ReferenceFilter<Class<Ruling>>>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aSelectionOnlyNeedsNamesForCardsMovedIntoHand() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    agent.runOperation("3 ProjectCard<Selecting>")
    tracking.expectArrivals(player, AcquiredCompany)
    agent.runOperation("ProjectCard<Hand FROM Selecting>")
    agent.runOperation("-2 ProjectCard<Selecting>")

    tracking.hand(player) shouldBe setOf(AcquiredCompany)
    tracking.requireComplete()
  }

  @Test
  internal fun trackedOfferRecordsItsChoicesAndChecksTheHandTransfer() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    val before = game.events.nextOrdinal
    tracking.expectArrivals(
        player,
        AcquiredCompany,
        AdaptedLichen,
        MicroMills,
        location = cn("Selecting"),
    )
    agent.runOperation("3 ProjectCard<Selecting>")
    tracking.expectDepartures(
        player,
        AdaptedLichen,
        MicroMills,
        location = cn("Selecting"),
    )
    agent.runOperation("-2 ProjectCard<Selecting>")
    tracking.expectArrivals(player, AcquiredCompany)
    agent.runOperation("ProjectCard<Hand FROM Selecting>")

    tracking.hand(player) shouldBe setOf(AcquiredCompany)
    game.events.changesSince(before).map { it.notes } shouldBe
        listOf(
            "Cards: $AcquiredCompany, $AdaptedLichen, $MicroMills",
            "Cards: $AdaptedLichen, $MicroMills",
            "Cards: $AcquiredCompany",
        )
    tracking.requireComplete()
  }

  @Test
  internal fun selectingOfferCannotReuseAHeldCardButCanOfferADiscardedCardAgain() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    tracking.expectArrivals(player, AdaptedLichen)
    agent.runOperation("ProjectCard")
    tracking.expectArrivals(player, AdaptedLichen, location = cn("Selecting"))
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> { agent.runOperation("ProjectCard<Selecting>") }
    game.events.nextOrdinal shouldBe before

    tracking.replacePendingArrivals(player, MicroMills, location = cn("Selecting"))
    agent.runOperation("ProjectCard<Selecting>")
    tracking.expectDepartures(player, MicroMills, location = cn("Selecting"))
    agent.runOperation("-ProjectCard<Selecting>")
    tracking.expectArrivals(player, MicroMills, location = cn("Selecting"))
    agent.runOperation("ProjectCard<Selecting>")

    tracking.hand(player) shouldBe setOf(AdaptedLichen)
    tracking.requireComplete()
  }

  @Test
  internal fun namedSelectingOfferCannotSilentlyLoseOrSubstituteACard() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    tracking.expectArrivals(
        player,
        AcquiredCompany,
        AdaptedLichen,
        location = cn("Selecting"),
    )
    agent.runOperation("2 ProjectCard<Selecting>")
    val before = game.events.nextOrdinal

    shouldThrow<InputRequiredException> { agent.runOperation("-ProjectCard<Selecting>") }
    tracking.expectArrivals(player, MicroMills)
    shouldThrow<IllegalStateException> {
      agent.runOperation("ProjectCard<Hand FROM Selecting>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.expectDepartures(player, AdaptedLichen, location = cn("Selecting"))
    agent.runOperation("-ProjectCard<Selecting>")
    tracking.replacePendingArrivals(player, AcquiredCompany)
    agent.runOperation("ProjectCard<Hand FROM Selecting>")
    tracking.hand(player) shouldBe setOf(AcquiredCompany)
    tracking.requireComplete()
  }

  @Test
  internal fun aPreludePlayedDirectlyFromSelectingUsesItsNamedOffer() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    tracking.expectArrivals(
        player,
        AlliedBank,
        back = cn("PreludeCard"),
        location = cn("Selecting"),
    )
    agent.runOperation("PreludeCard<Selecting>")
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agent.runOperation("$Donation FROM PreludeCard<Selecting>") { doTask("21 MC") }
    }
    game.events.nextOrdinal shouldBe before
    agent.runOperation("$AlliedBank FROM PreludeCard<Selecting>") { doTasks("PROD[4 MC]", "3 MC") }
    game.events.changesSince(before).mapNotNull { it.notes } shouldBe listOf("Cards: $AlliedBank")
    tracking.requireComplete()
  }

  @Test
  internal fun aFilteredSelectionRejectsTheWrongFreeCard() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, VenusNextExpansion, prelude2:cards, promos:cards, TurmoilExpansion"
        )
    val agent = agents[player]
    agent.runOperation("ProjectCard<Selecting>")
    tracking.expectArrivals(player, AcquiredCompany)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agent.runOperation("TakeSelectedCard<TagFilter<Class<VenusTag>>>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
  }

  @Test
  internal fun aFilteredSelectionAlsoAllowsAnOrdinaryPurchase() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, VenusNextExpansion, prelude2:cards, promos:cards, TurmoilExpansion"
        )
    val agent = agents[player]
    agent.runOperation("2 ProjectCard<Selecting>, 3 MC")
    tracking.expectArrivals(player, VenusGovernor)
    agent.runOperation("TakeSelectedCard<TagFilter<Class<VenusTag>>>")
    tracking.expectArrivals(player, AcquiredCompany)
    agent.runOperation("BuySelectedCards") {
      doTasks("3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    }

    tracking.hand(player) shouldBe setOf(VenusGovernor, AcquiredCompany)
    tracking.requireComplete()
  }

  @Test
  internal fun aRevealedHandCardReturnsWithoutANewArrival() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, AcquiredCompany, AdaptedLichen)
    agent.runOperation("2 ProjectCard")
    val before = game.events.nextOrdinal
    tracking.expectDepartures(player, AcquiredCompany, AdaptedLichen)

    agent.runOperation("2 ProjectCard<Revealed FROM Hand>")
    agent.runOperation("2 ProjectCard<Hand FROM Revealed>")

    tracking.hand(player) shouldBe setOf(AcquiredCompany, AdaptedLichen)
    game.events.changesSince(before).map { it.notes } shouldBe
        listOf("Cards: $AcquiredCompany, $AdaptedLichen", "Cards: $AcquiredCompany, $AdaptedLichen")
    tracking.requireComplete()
  }

  @Test
  internal fun cardAlreadyInAHandCannotBeDrawnAgain() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, AcquiredCompany)
    agents[player].runOperation("ProjectCard")
    tracking.expectArrivals(player, AcquiredCompany)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> { agents[player].runOperation("ProjectCard") }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(AcquiredCompany)
  }

  @Test
  internal fun cardCannotBeInTwoPlayersHands() {
    val config = GameConfig("PreludeExpansion", "Player1", "Player2")
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    val (first, second) = game.actors.filterIsInstance<Player>()
    val tracking = CardTracking.start(agents)
    val card = AcquiredCompany
    tracking.expectArrivals(first, card)
    agents[first].runOperation("ProjectCard")
    tracking.expectArrivals(second, card)
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> { agents[second].runOperation("ProjectCard") }

    game.events.nextOrdinal shouldBe before
    tracking.hand(first) shouldBe setOf(card)
    tracking.hand(second) shouldBe emptySet()
  }
}
