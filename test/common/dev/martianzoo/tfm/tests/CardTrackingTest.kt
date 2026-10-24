package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.AgentInputRequiredException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.engine.CardTracking.NamesRequiredException
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardTrackingTest : TfmTest() {
  private fun newTrackedGame(
      expansions: String = "PreludeExpansion",
      requireSelectingNames: Boolean = false,
  ): Pair<CardTracking, Player> {
    val config = GameConfig(expansions, "Player1")
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    val player = game.actors.filterIsInstance<Player>().single()
    return CardTracking.start(game, requireSelectingNames) to player
  }

  @Test
  internal fun aStoredCardCanBePlayedDirectlyFromItsTrackedStagingLocation() {
    val (tracking, player) = newTrackedGame("SelfReplicatingRobots")
    val agent = agents[player]
    admin.runOperation("Generation")
    agent.runOperation("SelfReplicatingRobots, 2 MC")
    tracking.expectArrivals(player, cn("Mine"))
    agent.runOperation("ProjectCard")

    agent.runOperation("UseAction<UseActionOnCardAction, Action1>") {
      doTask("UseAction<SelfReplicatingRobots, Action1>")

      doTask("StageForReplicatedProject<Class<Mine>>")
      autoExecNow(EAGER)
    }
    tracking.hand(player) shouldBe emptySet()
    agent.count("ProjectCard<StagedProject<Class<Mine>>>") shouldBe 1
    agent.count("RobotUnit<StagedProject<Class<Mine>>>") shouldBe 2

    agent.runOperation("UseAction<PlayCardAction, Action1>") {
      doTask("PlayCard<Class<ProjectCard>, Class<Mine>, StagedProject<Class<Mine>>>")

      autoExecNow(EAGER)
    }
    tracking.hand(player) shouldBe emptySet()
    agent.count("Mine") shouldBe 1
    agent.count("RobotUnit") shouldBe 0
    agent.count("MC") shouldBe 0
    val afterPlay = game.events.nextOrdinal
    tracking.expectArrivals(player, cn("Mine"))
    shouldThrow<IllegalStateException> { agent.runOperation("ProjectCard") }
    game.events.nextOrdinal shouldBe afterPlay
    tracking.replacePendingArrivals(player)
    tracking.requireComplete()
  }

  @Test
  internal fun stagingIdentifiesWhichCardLeftTheTrackedHand() {
    val (tracking, player) = newTrackedGame("SelfReplicatingRobots")
    val agent = agents[player]
    admin.runOperation("Generation")
    agent.runOperation("SelfReplicatingRobots, 2 MC")
    tracking.expectArrivals(player, cn("Mine"), cn("TitaniumMine"))
    agent.runOperation("ProjectCard")
    agent.runOperation("ProjectCard")

    agent.runOperation("UseAction<UseActionOnCardAction, Action1>") {
      doTask("UseAction<SelfReplicatingRobots, Action1>")

      doTask("StageForReplicatedProject<Class<Mine>>")
      autoExecNow(EAGER)
    }
    tracking.hand(player) shouldBe setOf(cn("TitaniumMine"))
    val afterStaging = game.events.nextOrdinal
    shouldThrow<IllegalStateException> {
      agent.runOperation("UseAction<PlayCardAction, Action1>") {
        doTask("PlayCard<Class<ProjectCard>, Class<Mine>, Hand>")
        autoExecNow(EAGER)
      }
    }
    game.events.nextOrdinal shouldBe afterStaging
    agent.count("ProjectCard<StagedProject<Class<Mine>>>") shouldBe 1
    tracking.requireComplete()
  }

  @Test
  internal fun trackedHandsFollowBackwardAndForwardSeeking() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AcquiredCompany"), cn("AdaptedLichen"))
    agent.runOperation("ProjectCard")
    val first = game.timeline.checkpoint()
    agent.runOperation("ProjectCard")
    val second = game.timeline.checkpoint()
    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"), cn("AdaptedLichen"))

    game.timeline.seek(first)
    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
    game.timeline.seek(second)
    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"), cn("AdaptedLichen"))
    tracking.requireComplete()
  }

  @Test
  internal fun speculativeAgentsDrawAnonymousCardsWithoutChangingTrackedSource() {
    val (sourceTracking, player) = newTrackedGame()
    val firstCard = cn("AcquiredCompany")
    sourceTracking.expectArrivals(player, firstCard)
    agents[player].runOperation("ProjectCard")

    val imagined = agents.overlay()
    imagined.world.agentTransactionGuard shouldBe null
    imagined[player].runOperation("ProjectCard")

    val copied = imagined.fork()
    copied.world.agentTransactionGuard shouldBe null
    copied[player].count("ProjectCard<Hand>") shouldBe 2
    sourceTracking.requireComplete()

    imagined.reset()
    sourceTracking.hand(player) shouldBe setOf(firstCard)
    imagined[player].count("ProjectCard<Hand>") shouldBe 1
    imagined[player].runOperation("ProjectCard")
    imagined[player].count("ProjectCard<Hand>") shouldBe 2
  }

  @Test
  internal fun fullCopyOfTrackedWorldCanDrawAnonymousCards() {
    val (sourceTracking, player) = newTrackedGame()
    val firstCard = cn("AcquiredCompany")
    sourceTracking.expectArrivals(player, firstCard)
    agents[player].runOperation("ProjectCard")

    val copied = agents.fork()
    copied.world.agentTransactionGuard shouldBe null
    copied[player].runOperation("ProjectCard")

    sourceTracking.requireComplete()
    sourceTracking.hand(player) shouldBe setOf(firstCard)
    copied[player].count("ProjectCard<Hand>") shouldBe 2
  }

  @Test
  internal fun cardTrackingCannotBeStartedOnAnOverlay() {
    val config = GameConfig("PreludeExpansion", "Player1")
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    val player = game.actors.filterIsInstance<Player>().single()
    val imagined = agents.overlay()

    shouldThrow<IllegalStateException> { CardTracking.start(imagined.world) }
    imagined[player].runOperation("ProjectCard")
    imagined[player].count("ProjectCard<Hand>") shouldBe 1
    agents[player].count("ProjectCard<Hand>") shouldBe 0
  }

  @Test
  internal fun aDifferentChangeAtTheSameOrdinalDoesNotReuseDiscardedCardLocations() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("ProjectCard")
    val beforeDiscard = game.timeline.checkpoint()
    tracking.expectDepartures(player, cn("AcquiredCompany"))
    agent.runOperation("-ProjectCard")
    val discarded = game.events.nextOrdinal
    tracking.hand(player) shouldBe emptySet()

    game.timeline.seek(beforeDiscard)
    game.timeline.truncate()
    agent.runOperation("MC")

    game.events.nextOrdinal shouldBe discarded
    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
    agent.count("MC") shouldBe 1
  }

  @Test
  internal fun unnamedHandGainFailsWithoutCommitting() {
    val (_, player) = newTrackedGame()
    val before = game.events.nextOrdinal

    shouldThrow<AgentInputRequiredException> { agents[player].runOperation("ProjectCard") }

    game.events.nextOrdinal shouldBe before
    agents[player].count("ProjectCard<Hand>") shouldBe 0
  }

  @Test
  internal fun unnamedHandRemovalFailsWithoutCommitting() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AdaptedLichen"))
    agent.runOperation("ProjectCard")
    val before = game.events.nextOrdinal

    shouldThrow<AgentInputRequiredException> { agent.runOperation("-ProjectCard") }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(cn("AdaptedLichen"))
  }

  @Test
  internal fun corporationAndPreludeHandMovementsAlsoNeedNames() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    val before = game.events.nextOrdinal

    shouldThrow<AgentInputRequiredException> { agent.runOperation("CorporationCard") }
    shouldThrow<AgentInputRequiredException> { agent.runOperation("PreludeCard") }

    game.events.nextOrdinal shouldBe before
    tracking.expectArrivals(player, cn("Ecoline"), back = cn("CorporationCard"))
    tracking.expectArrivals(player, cn("Biolab"), back = cn("PreludeCard"))
    agent.runOperation("CorporationCard")
    agent.runOperation("PreludeCard")
    tracking.expectDepartures(player, cn("Ecoline"), back = cn("CorporationCard"))
    agent.runOperation("-CorporationCard")

    tracking.hand(player) shouldBe setOf(cn("Biolab"))
    game.events.changesSince(before).map { it.notes } shouldBe
        listOf("Cards: Ecoline", "Cards: Biolab", "Cards: Ecoline")
    tracking.requireComplete()
  }

  @Test
  internal fun suppliedNamesMustBeConcreteCardFrontsWithTheRightBack() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)

    shouldThrow<IllegalArgumentException> {
      tracking.expectArrivals(player, cn("NeverPrintedCard"))
    }
    shouldThrow<IllegalArgumentException> { tracking.expectArrivals(player, cn("MC")) }
    shouldThrow<IllegalArgumentException> { tracking.expectArrivals(player, cn("CardFront")) }
    shouldThrow<IllegalArgumentException> { tracking.expectArrivals(player, cn("Ecoline")) }
    shouldThrow<IllegalArgumentException> {
      tracking.expectDepartures(player, cn("Biolab"), back = cn("CorporationCard"))
    }
    shouldThrow<IllegalArgumentException> {
      tracking.expectArrivals(
          player,
          cn("Biolab"),
          back = cn("ProjectCard"),
          location = cn("Selecting"),
      )
    }
    shouldThrow<IllegalArgumentException> {
      tracking.expectDepartures(player, cn("CardFront"), location = cn("Selecting"))
    }
  }

  @Test
  internal fun suppliedNameCountMustMatchTheActualMovement() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    val before = game.events.nextOrdinal

    shouldThrow<AgentInputRequiredException> { agents[player].runOperation("2 ProjectCard") }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe emptySet()
    agents[player].runOperation("ProjectCard")
    tracking.requireComplete()
  }

  @Test
  internal fun unusedSuppliedNamesFailTheFinalCountCheck() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("AcquiredCompany"), cn("AdaptedLichen"))

    agents[player].runOperation("ProjectCard")

    shouldThrow<IllegalStateException> { tracking.requireComplete() }
  }

  @Test
  internal fun rejectedArrivalNameCanBeCorrectedWithoutChangingRecordedNames() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AdaptedLichen"))
    agent.runOperation("ProjectCard")
    tracking.expectArrivals(player, cn("AcquiredCompany"))

    shouldThrow<IllegalStateException> {
      agent.runOperation("SearchForCard<TagFilter<Class<PlantTag>>>")
    }
    tracking.replacePendingArrivals(player, cn("EcologicalZone"))
    agent.runOperation("SearchForCard<TagFilter<Class<PlantTag>>>")

    tracking.hand(player) shouldBe setOf(cn("AdaptedLichen"), cn("EcologicalZone"))
    tracking.requireComplete()
  }

  @Test
  internal fun rejectedDepartureNameCanBeCorrected() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AdaptedLichen"))
    agent.runOperation("ProjectCard")
    tracking.expectDepartures(player, cn("AcquiredCompany"))

    shouldThrow<IllegalStateException> { agent.runOperation("-ProjectCard") }
    tracking.replacePendingDepartures(player, cn("AdaptedLichen"))
    agent.runOperation("-ProjectCard")

    tracking.hand(player) shouldBe emptySet()
    tracking.requireComplete()
  }

  @Test
  internal fun aNamedCorporationPlayMustComeFromTheCorporationHand() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("Ecoline"), back = cn("CorporationCard"))
    val agent = agents[player]
    agent.runOperation("CorporationCard")
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> { agent.runOperation("CrediCor FROM CorporationCard") }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(cn("Ecoline"))
  }

  @Test
  internal fun astraMechanicaReturnsLawSuitFromTheAttackersEventPileToHand() {
    val config =
        GameConfig(
            "PreludeExpansion, LawSuit, AstraMechanica, MineralDeposit",
            "Player1",
            "Player2",
        )
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    val (holder, attacker) = game.actors.filterIsInstance<Player>()
    val tracking = CardTracking.start(game)
    admin.runOperation("Generation")
    agents[attacker].runOperation("3 MC")
    agents[holder].runOperation("ProductionWasAttacked<$holder, Class<Energy>, $attacker>")
    tracking.expectArrivals(holder, cn("LawSuit"))
    agents[holder].runOperation("ProjectCard")
    val before = game.events.nextOrdinal

    agents[holder].runOperation("LawSuit FROM ProjectCard") {
      selectOnlyTask()
      doTask("3 MC<$holder FROM $attacker>")
      selectTask("PlayedEvent<$attacker, Class<LawSuit>> FROM LawSuit!")
    }

    game.events.changesSince(before).mapNotNull { it.notes } shouldBe listOf("Cards: LawSuit")
    agents[attacker].count("PlayedEvent<$attacker, Class<LawSuit>>") shouldBe 1
    tracking.expectArrivals(attacker, cn("MineralDeposit"), cn("AstraMechanica"))
    agents[attacker].runOperation("2 ProjectCard")
    agents[attacker].runOperation("MineralDeposit FROM ProjectCard")
    val beforeAstra = game.events.nextOrdinal

    agents[attacker].runOperation("AstraMechanica FROM ProjectCard") {
      doTask("ProjectCard FROM PlayedEvent<Class<LawSuit>>")
    }

    tracking.hand(attacker) shouldBe setOf(cn("LawSuit"), cn("MineralDeposit"))
    tracking.hand(holder) shouldBe emptySet()
    game.events.changesSince(beforeAstra).mapNotNull { it.notes } shouldBe
        listOf("Cards: AstraMechanica", "Cards: LawSuit", "Cards: MineralDeposit")
    tracking.requireComplete()
  }

  @Test
  internal fun astraMechanicaCannotTakePharmacyUnionInATrackedGame() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, AstraMechanica, PharmacyUnion, MineralDeposit, BribedCommittee"
        )
    val agent = agents[player]
    tracking.expectArrivals(
        player,
        cn("MineralDeposit"),
        cn("BribedCommittee"),
        cn("AstraMechanica"),
    )
    agent.runOperation("3 ProjectCard")
    agent.runOperation("MineralDeposit FROM ProjectCard")
    agent.runOperation("BribedCommittee FROM ProjectCard")
    // Pharmacy Union can become a PlayedEvent, but it is a corporation rather than an EventCard.
    agent.sneak("PlayedEvent<Class<PharmacyUnion>>")
    val before = game.events.nextOrdinal

    shouldThrow<TaskException> {
      agent.runOperation("AstraMechanica FROM ProjectCard") {
        doTask("ProjectCard FROM PlayedEvent<Class<PharmacyUnion>>")
      }
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(cn("AstraMechanica"))
    agent.runOperation("AstraMechanica FROM ProjectCard") {
      doTask("ProjectCard FROM PlayedEvent<Class<MineralDeposit>>")
    }
    tracking.hand(player) shouldBe setOf(cn("MineralDeposit"), cn("BribedCommittee"))
    agent.count("PlayedEvent<Class<PharmacyUnion>>") shouldBe 1
    tracking.requireComplete()
  }

  @Test
  internal fun automaticDrawWaitsForItsName() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    agents.taskLog.start()
    agent.autoExecPolicy = NONE
    val taskId = agent.addTasks("ProjectCard").single()
    val before = game.events.nextOrdinal

    agent.autoExecPolicy = EAGER

    game.events.nextOrdinal shouldBe before
    game.tasks.ids() shouldBe setOf(taskId)
    tracking.expectArrivals(player, cn("AdaptedLichen"))
    agent.autoExecNow()

    tracking.hand(player) shouldBe setOf(cn("AdaptedLichen"))
    game.events.changesSince(before).single().notes shouldBe "Cards: AdaptedLichen"
    tracking.requireComplete()
  }

  @Test
  internal fun discardedCardMayEnterTheHandAgain() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    val card = cn("AdaptedLichen")
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
    tracking.expectArrivals(player, cn("AdaptedLichen"))
    agent.runOperation("ProjectCard")
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agent.runOperation("AcquiredCompany FROM ProjectCard")
    }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(cn("AdaptedLichen"))
  }

  @Test
  internal fun aResourceExchangeUsesTheNamedHandDeparture() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    val card = cn("AdaptedLichen")
    tracking.expectArrivals(player, card)
    agent.runOperation("ProjectCard")
    tracking.expectDepartures(player, card)

    agent.runOperation("MC FROM ProjectCard")

    tracking.hand(player) shouldBe emptySet()
    tracking.requireComplete()
  }

  @Test
  internal fun anUnrelatedAutomaticTaskCanFinishWhileTheCardTaskWaits() {
    val (_, player) = newTrackedGame()
    val agent = agents[player]
    agent.autoExecPolicy = NONE
    agent.addTasks("ProjectCard, MC")

    agent.autoExecPolicy = EAGER

    agent.count("MC") shouldBe 1
    agent.count("ProjectCard<Hand>") shouldBe 0
    game.tasks.ids().size shouldBe 1
  }

  @Test
  internal fun aFilteredArrivalMustHaveThePrintedTag() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("AcquiredCompany"))
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
    tracking.expectArrivals(player, cn("AdaptedLichen"), cn("AcquiredCompany"))
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
    tracking.expectArrivals(player, cn("MicroMills"), cn("ResearchCoordination"))
    agents[player].runOperation("2 SearchForCard<NoTagsFilter>")

    tracking.hand(player) shouldBe setOf(cn("MicroMills"), cn("ResearchCoordination"))
    tracking.requireComplete()
  }

  @Test
  internal fun anEventIconCountsAsAPrintedTag() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("MiningExpedition"))
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
    tracking.expectArrivals(player, cn("MassConverter"), cn("Algae"))
    agents[player].runOperation("2 SearchForCard<RequirementFilter>")

    tracking.hand(player) shouldBe setOf(cn("MassConverter"), cn("Algae"))
    tracking.requireComplete()
  }

  @Test
  internal fun aRequirementFilterRejectsCardsWithoutRequirements() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("AcquiredCompany"))
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
    tracking.expectArrivals(player, cn("Dirigibles"))
    agents[player].runOperation("SearchForCard<ReferenceFilter<Class<Floater>>>")

    tracking.hand(player) shouldBe setOf(cn("Dirigibles"))
    tracking.requireComplete()
  }

  @Test
  internal fun aReferenceFilterRejectsAnUnrelatedCard() {
    val (tracking, player) = newTrackedGame("PreludeExpansion, VenusNextExpansion")
    tracking.expectArrivals(player, cn("VenusGovernor"))
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
    tracking.expectArrivals(player, cn("SupportedResearch"))
    agents[player].runOperation("SearchForCard<ReferenceFilter<Class<Ruling>>>")

    tracking.hand(player) shouldBe setOf(cn("SupportedResearch"))
    tracking.requireComplete()
  }

  @Test
  internal fun aPartyRequirementSearchRejectsACardThatOnlyMovesDelegates() {
    val (tracking, player) =
        newTrackedGame(
            "PreludeExpansion, VenusNextExpansion, prelude2:cards, promos:cards, TurmoilExpansion"
        )
    tracking.expectArrivals(player, cn("BannedDelegate"))
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
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("ProjectCard<Hand FROM Selecting>")
    agent.runOperation("-2 ProjectCard<Selecting>")

    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
    tracking.requireComplete()
  }

  @Test
  internal fun trackedOfferRecordsItsChoicesAndChecksTheHandTransfer() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    val before = game.events.nextOrdinal
    tracking.expectArrivals(
        player,
        cn("AcquiredCompany"),
        cn("AdaptedLichen"),
        cn("MicroMills"),
        location = cn("Selecting"),
    )
    agent.runOperation("3 ProjectCard<Selecting>")
    tracking.expectDepartures(
        player,
        cn("AdaptedLichen"),
        cn("MicroMills"),
        location = cn("Selecting"),
    )
    agent.runOperation("-2 ProjectCard<Selecting>")
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("ProjectCard<Hand FROM Selecting>")

    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
    game.events.changesSince(before).map { it.notes } shouldBe
        listOf(
            "Cards: AcquiredCompany, AdaptedLichen, MicroMills",
            "Cards: AdaptedLichen, MicroMills",
            "Cards: AcquiredCompany",
        )
    tracking.requireComplete()
  }

  @Test
  internal fun missingNamesReportTheMovementAndTheCardsThatCouldMove() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    val selecting = cn("Selecting")

    val dealt =
        shouldThrow<NamesRequiredException> { agent.runOperation("3 ProjectCard<Selecting>") }
    dealt.arriving shouldBe true
    dealt.location shouldBe selecting
    dealt.count shouldBe 3
    dealt.candidates shouldBe null

    // The offered cards exist only inside the rolled-back attempt; they are still the candidates.
    tracking.expectArrivals(
        player,
        cn("AcquiredCompany"),
        cn("AdaptedLichen"),
        cn("MicroMills"),
        location = selecting,
    )
    tracking.expectDepartures(player, cn("AdaptedLichen"), location = selecting)
    val rejected =
        shouldThrow<NamesRequiredException> {
          agent.sneak("3 ProjectCard<Selecting>, -2 ProjectCard<Selecting>")
        }
    rejected.arriving shouldBe false
    rejected.count shouldBe 1
    rejected.candidates shouldBe setOf(cn("AcquiredCompany"), cn("MicroMills"))

    tracking.expectDepartures(player, cn("MicroMills"), location = selecting)
    agent.sneak("3 ProjectCard<Selecting>, -2 ProjectCard<Selecting>")
    tracking.hand(player, location = selecting) shouldBe setOf(cn("AcquiredCompany"))

    val kept =
        shouldThrow<NamesRequiredException> {
          agent.runOperation("ProjectCard<Hand FROM Selecting>")
        }
    kept.arriving shouldBe true
    kept.candidates shouldBe setOf(cn("AcquiredCompany"))
  }

  @Test
  internal fun selectingOfferCannotReuseAHeldCardButCanOfferADiscardedCardAgain() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AdaptedLichen"))
    agent.runOperation("ProjectCard")
    tracking.expectArrivals(player, cn("AdaptedLichen"), location = cn("Selecting"))
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> { agent.runOperation("ProjectCard<Selecting>") }
    game.events.nextOrdinal shouldBe before

    tracking.replacePendingArrivals(player, cn("MicroMills"), location = cn("Selecting"))
    agent.runOperation("ProjectCard<Selecting>")
    tracking.expectDepartures(player, cn("MicroMills"), location = cn("Selecting"))
    agent.runOperation("-ProjectCard<Selecting>")
    tracking.expectArrivals(player, cn("MicroMills"), location = cn("Selecting"))
    agent.runOperation("ProjectCard<Selecting>")

    tracking.hand(player) shouldBe setOf(cn("AdaptedLichen"))
    tracking.requireComplete()
  }

  @Test
  internal fun selectingNamesRequireSelectingTrackingAtGameStart() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    agent.runOperation("ProjectCard<Selecting>")
    shouldThrow<IllegalArgumentException> {
      tracking.expectArrivals(player, cn("AcquiredCompany"), location = cn("Selecting"))
    }
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("ProjectCard<Hand FROM Selecting>")

    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
    tracking.requireComplete()
  }

  @Test
  internal fun namedSelectingOfferCannotSilentlyLoseOrSubstituteACard() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    tracking.expectArrivals(
        player,
        cn("AcquiredCompany"),
        cn("AdaptedLichen"),
        location = cn("Selecting"),
    )
    agent.runOperation("2 ProjectCard<Selecting>")
    val before = game.events.nextOrdinal

    shouldThrow<AgentInputRequiredException> { agent.runOperation("-ProjectCard<Selecting>") }
    tracking.expectArrivals(player, cn("MicroMills"))
    shouldThrow<IllegalStateException> {
      agent.runOperation("ProjectCard<Hand FROM Selecting>")
    }

    game.events.nextOrdinal shouldBe before
    tracking.expectDepartures(player, cn("AdaptedLichen"), location = cn("Selecting"))
    agent.runOperation("-ProjectCard<Selecting>")
    tracking.replacePendingArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("ProjectCard<Hand FROM Selecting>")
    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
    tracking.requireComplete()
  }

  @Test
  internal fun strictSelectingModeRequiresNamesBeforeTheOfferCompletes() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    val before = game.events.nextOrdinal

    shouldThrow<AgentInputRequiredException> { agent.runOperation("2 ProjectCard<Selecting>") }
    game.events.nextOrdinal shouldBe before
    tracking.expectArrivals(player, cn("AcquiredCompany"), location = cn("Selecting"))
    shouldThrow<AgentInputRequiredException> { agent.runOperation("2 ProjectCard<Selecting>") }
    game.events.nextOrdinal shouldBe before
    tracking.replacePendingArrivals(
        player,
        cn("AcquiredCompany"),
        cn("AdaptedLichen"),
        location = cn("Selecting"),
    )
    agent.runOperation("2 ProjectCard<Selecting>")
    tracking.expectDepartures(player, cn("AdaptedLichen"), location = cn("Selecting"))
    agent.runOperation("-ProjectCard<Selecting>")
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("ProjectCard<Hand FROM Selecting>")
    tracking.requireComplete()
  }

  @Test
  internal fun strictSelectingTaskWaitsForNamesWhileOtherAutomaticWorkRuns() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    agent.autoExecPolicy = NONE
    agent.addTasks("ProjectCard<Selecting>, MC")

    agent.autoExecPolicy = EAGER

    agent.count("MC") shouldBe 1
    agent.count("ProjectCard<Selecting>") shouldBe 0
    tracking.expectArrivals(player, cn("AcquiredCompany"), location = cn("Selecting"))
    agent.autoExecNow()
    agent.count("ProjectCard<Selecting>") shouldBe 1
    tracking.requireComplete()
  }

  @Test
  internal fun aPreludePlayedDirectlyFromSelectingUsesItsNamedOffer() {
    val (tracking, player) = newTrackedGame(requireSelectingNames = true)
    val agent = agents[player]
    tracking.expectArrivals(
        player,
        cn("AlliedBank"),
        back = cn("PreludeCard"),
        location = cn("Selecting"),
    )
    agent.runOperation("PreludeCard<Selecting>")
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      agent.runOperation("Biolab FROM PreludeCard<Selecting>")
    }
    game.events.nextOrdinal shouldBe before
    agent.runOperation("AlliedBank FROM PreludeCard<Selecting>")
    game.events.changesSince(before).mapNotNull { it.notes } shouldBe listOf("Cards: AlliedBank")
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
    tracking.expectArrivals(player, cn("AcquiredCompany"))
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
    val agent = game.testTfm(player)
    agent.runOperation("2 ProjectCard<Selecting>, 3 MC")
    tracking.expectArrivals(player, cn("VenusGovernor"))
    agent.runOperation("TakeSelectedCard<TagFilter<Class<VenusTag>>>")
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agent.runOperation("BuySelectedCards")

    tracking.hand(player) shouldBe setOf(cn("VenusGovernor"), cn("AcquiredCompany"))
    tracking.requireComplete()
  }

  @Test
  internal fun aRevealedHandCardReturnsWithoutANewArrival() {
    val (tracking, player) = newTrackedGame()
    val agent = agents[player]
    tracking.expectArrivals(player, cn("AcquiredCompany"), cn("AdaptedLichen"))
    agent.runOperation("2 ProjectCard")
    val before = game.events.nextOrdinal
    tracking.expectDepartures(player, cn("AcquiredCompany"), cn("AdaptedLichen"))

    agent.runOperation("2 ProjectCard<Revealed FROM Hand>")
    agent.runOperation("2 ProjectCard<Hand FROM Revealed>")

    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"), cn("AdaptedLichen"))
    game.events.changesSince(before).map { it.notes } shouldBe
        listOf("Cards: AcquiredCompany, AdaptedLichen", "Cards: AcquiredCompany, AdaptedLichen")
    tracking.requireComplete()
  }

  @Test
  internal fun laterValidationPreservesOtherEventNotesInPlace() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    val before = game.events.nextOrdinal
    agents[player].runOperation("ProjectCard")
    val event = game.events.changesSince(before).single()
    event.notes = "Source: archive\n${event.notes}\nReview: checked separately"

    tracking.requireComplete()

    event.notes shouldBe "Source: archive\nCards: AcquiredCompany\nReview: checked separately"
  }

  @Test
  internal fun cardAlreadyInAHandCannotBeDrawnAgain() {
    val (tracking, player) = newTrackedGame()
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    agents[player].runOperation("ProjectCard")
    tracking.expectArrivals(player, cn("AcquiredCompany"))
    val before = game.events.nextOrdinal

    shouldThrow<IllegalStateException> { agents[player].runOperation("ProjectCard") }

    game.events.nextOrdinal shouldBe before
    tracking.hand(player) shouldBe setOf(cn("AcquiredCompany"))
  }

  @Test
  internal fun cardCannotBeInTwoPlayersHands() {
    val config = GameConfig("PreludeExpansion", "Player1", "Player2")
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    val (first, second) = game.actors.filterIsInstance<Player>()
    val tracking = CardTracking.start(game)
    val card = cn("AcquiredCompany")
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
