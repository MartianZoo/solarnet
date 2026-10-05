package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameConfig
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.tfm.tests.cards.cardnames.AcquiredCompany
import dev.martianzoo.tfm.tests.cards.cardnames.AdaptedLichen
import dev.martianzoo.tfm.tests.cards.cardnames.AsteroidMining
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardTrackingFullGameTestTest : CardTrackingFullGameTest() {
  override val config = GameConfig("PreludeExpansion", "Player1")
  internal override val producesReplayRecording = false

  @Test
  internal fun namedDrawsReturnsPlaysAndDiscardsMaintainThePlayersHand() {
    val checkpoint = game.timeline.checkpoint()
    p1.runOperation("3 ProjectCard") {
      val drawEvent = game.events.changesSince(checkpoint).single()
      drawEvent.notes = "Source: test archive"
      p1.draw(AcquiredCompany)
      drawEvent.notes += "\nReview: Cards: AcquiredCompany was checked separately"
      p1.draw(AdaptedLichen, AsteroidMining)
    }
    p1.cardsHand shouldBe setOf(AcquiredCompany, AdaptedLichen, AsteroidMining)

    p1.runOperation("$AcquiredCompany FROM ProjectCard")
    shouldThrow<IllegalStateException> { p1.draw(AcquiredCompany) }
    p1.runOperation("ProjectCard") { p1.returnToHand(AcquiredCompany) }
    p1.runOperation("-2 ProjectCard") { p1.discard(AcquiredCompany, AdaptedLichen) }

    p1.cardsHand shouldBe setOf(AsteroidMining)
    assertCardTrackingComplete()

    val annotatedChanges = game.events.changesSince(checkpoint).filter { it.isProjectCardChange() }
    annotatedChanges.map { it.notes } shouldBe
        listOf(
            "Source: test archive\nCards: AcquiredCompany, AdaptedLichen, AsteroidMining\n" +
                "Review: Cards: AcquiredCompany was checked separately",
            "Cards: AcquiredCompany",
            "Cards: AcquiredCompany",
            "Cards: AcquiredCompany, AdaptedLichen",
        )
    annotatedChanges.first() shouldBe annotatedChanges.first().copy()
  }

  @Test
  internal fun discardAndDrawNameTheirSeparateEvents() {
    p1.runOperation("ProjectCard") { p1.draw(AcquiredCompany) }
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("-ProjectCard THEN ProjectCard") {
      p1.discard(AcquiredCompany)
      p1.draw(AdaptedLichen)
    }

    p1.cardsHand shouldBe setOf(AdaptedLichen)
    assertCardTrackingComplete()
    game.events
        .changesSince(checkpoint)
        .filter { it.isProjectCardChange() }
        .map { it.notes } shouldBe listOf("Cards: AcquiredCompany", "Cards: AdaptedLichen")
  }

  @Test
  internal fun revealingHandCardsRequiresNamesForBothMovements() {
    p1.runOperation("2 ProjectCard") { p1.draw(AcquiredCompany, AdaptedLichen) }
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("2 ProjectCard<Revealed FROM Hand>")
    p1.runOperation("2 ProjectCard<Hand FROM Revealed>")

    p1.cardsHand shouldBe setOf(AcquiredCompany, AdaptedLichen)
    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }
    p1.nameRevealedCards(AcquiredCompany, AdaptedLichen)
    assertCardTrackingComplete()
    game.events
        .changesSince(checkpoint)
        .filter { it.isProjectCardChange() }
        .map { it.notes } shouldBe
        listOf(
            "Cards: AcquiredCompany, AdaptedLichen",
            "Cards: AcquiredCompany, AdaptedLichen",
        )
  }

  @Test
  internal fun onlyTheKeptCardInASelectionNeedsAnIdentity() {
    p1.runOperation("3 ProjectCard<Selecting>")
    p1.runOperation("ProjectCard<Hand FROM Selecting>") { p1.draw(AcquiredCompany) }
    p1.runOperation("-2 ProjectCard<Selecting>")

    p1.cardsHand shouldBe setOf(AcquiredCompany)
    assertCardTrackingComplete()
  }

  @Test
  internal fun strictCompletionRequiresBothExchangeSidesToBeNamed() {
    p1.runOperation("ProjectCard") { p1.draw(AcquiredCompany) }
    p1.runOperation("-ProjectCard THEN ProjectCard")

    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }
  }

  @Test
  internal fun arrivalOrderNamesTheDrawAfterADiscard() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany, AdaptedLichen))
    replay.setUp()
    replay.gainToHand()

    replay.exchangeFromHand(AcquiredCompany)

    replay.hand() shouldBe setOf(AdaptedLichen)
    replay.assertComplete()
    replay.projectCardNotes() shouldBe
        listOf("Cards: AcquiredCompany", "Cards: AcquiredCompany", "Cards: AdaptedLichen")
  }

  @Test
  internal fun cardNamesCanBeRecordedBeforeTheCorrespondingChange() {
    val checkpoint = game.timeline.checkpoint()

    p1.draw(AcquiredCompany)
    p1.runOperation("ProjectCard")
    assertCardTrackingComplete()

    game.events.changesSince(checkpoint).single { it.isProjectCardChange() }.notes shouldBe
        "Cards: AcquiredCompany"
  }

  @Test
  internal fun completionFailsWhenANamedChangeNeverOccurs() {
    p1.draw(AcquiredCompany)

    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }
  }

  @Test
  internal fun completionFailsWhenAHandChangeWasNotNamed() {
    p1.runOperation("ProjectCard")

    shouldThrow<AssertionError> { assertCardTrackingComplete() }
  }

  @Test
  internal fun strictCompletionFailsWhenAnonymousChangesLeaveNoHandMismatch() {
    p1.runOperation("ProjectCard")
    p1.runOperation("-ProjectCard")

    shouldThrow<IllegalStateException> { assertCardTrackingComplete() }
  }

  @Test
  internal fun arrivalOrderChecksFilteredDraws() {
    val replay = ArrivalOrderReplay(listOf(AdaptedLichen))
    replay.setUp()
    replay.gainToHand(filter = "TagFilter<Class<PlantTag>>")

    replay.assertComplete()
  }

  @Test
  internal fun arrivalOrderRejectsACardThatDoesNotMatchTheFilter() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany))
    replay.setUp()
    replay.gainToHand(filter = "TagFilter<Class<PlantTag>>")

    shouldThrow<IllegalStateException> { replay.assertComplete() }
  }

  @Test
  internal fun arrivalOrderNamesCardsAsTheyEnterAndLeaveTheHand() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany, AdaptedLichen))
    replay.setUp()

    replay.gainToHand(2)
    replay.discardFromHand(AdaptedLichen)

    replay.hand() shouldBe setOf(AcquiredCompany)
    replay.assertComplete()
    replay.projectCardNotes() shouldBe
        listOf(
            "Cards: AcquiredCompany, AdaptedLichen",
            "Cards: AdaptedLichen",
        )
  }

  @Test
  internal fun arrivalOrderRejectsDiscardingACardThatNeverEnteredTheHand() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany))
    replay.setUp()
    replay.gainToHand()

    shouldThrow<IllegalStateException> { replay.discardFromHand(AdaptedLichen) }
    replay.assertComplete()
  }

  @Test
  internal fun arrivalOrderFailsWhenAnEventNeedsMoreCardsThanRemain() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany))
    replay.setUp()
    replay.gainToHand(2)

    shouldThrow<IllegalStateException> { replay.hand() }
  }

  @Test
  internal fun arrivalOrderFailsWhenCardsRemainUnused() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany, AdaptedLichen))
    replay.setUp()
    replay.gainToHand()

    shouldThrow<IllegalStateException> { replay.assertComplete() }
  }

  @Test
  internal fun arrivalOrderRejectsTheSamePhysicalCardTwice() {
    val replay = ArrivalOrderReplay(listOf(AcquiredCompany, AcquiredCompany))

    shouldThrow<IllegalStateException> { replay.setUp() }
  }

  private fun ChangeEvent.isProjectCardChange(): Boolean =
      change.gaining?.className == PROJECT_CARD || change.removing?.className == PROJECT_CARD

  private class ArrivalOrderReplay(arrivals: List<ClassName>) : CardTrackingFullGameTest() {
    override val config = GameConfig("PreludeExpansion", "Player1")
    override val projectCardArrivalOrder = mapOf(cn("Player1") to arrivals)

    fun setUp() = commonSetup()

    fun gainToHand(count: Int = 1, filter: String? = null) {
      val draw = if (filter == null) "ProjectCard" else "SearchForCard<$filter>"
      p1.runOperation("$count $draw")
    }

    fun discardFromHand(card: ClassName) {
      check(card in p1.cardsHand) { "$card never entered the hand" }
      p1.runOperation("-ProjectCard") { p1.discard(card) }
    }

    fun exchangeFromHand(card: ClassName) {
      p1.runOperation("-ProjectCard THEN ProjectCard") { p1.discard(card) }
    }

    fun hand(): Set<ClassName> = p1.cardsHand

    fun assertComplete() = assertCardTrackingComplete()

    fun projectCardNotes(): List<String?> =
        game.events
            .changesSince(Checkpoint(0))
            .filter {
              it.change.gaining?.className == PROJECT_CARD ||
                  it.change.removing?.className == PROJECT_CARD
            }
            .map { it.notes }
  }

  private companion object {
    val PROJECT_CARD = cn("ProjectCard")
  }
}
