package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.generated.CardFront
import dev.martianzoo.generated.Class as PetsClass
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class CardTrackingFullGameTest(
    private val requireEveryProjectCardChangeNamed: Boolean = false,
) : AbstractFullGameTest() {
  /** Source-known card identities in the order they enter each Player's modeled card state. */
  protected open val projectCardArrivalOrder: Map<ClassName, List<ClassName>> = emptyMap()

  private val cards = linkedMapOf<ClassName, CardLocation>()
  private val arrivalOffsets = mutableMapOf<ClassName, Int>()
  private val projectCardEvents = mutableListOf<ChangeEvent>()
  private val eventCards = mutableMapOf<ChangeEvent, MutableList<ClassName>>()
  private val recentProjectCardStarts = mutableMapOf<Player, Int>()
  private var nextUnknownProjectCard = 1
  // Replay narration can identify a card immediately before or after the engine records its move.
  private val pendingAnnotations = mutableListOf<PendingAnnotation>()
  private lateinit var trackingCheckpoint: Checkpoint
  private var trackingStartOrdinal: Int = 0

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()
    cards.clear()
    arrivalOffsets.clear()
    projectCardEvents.clear()
    eventCards.clear()
    recentProjectCardStarts.clear()
    pendingAnnotations.clear()
    nextUnknownProjectCard = 1
    trackingCheckpoint = game.timeline.checkpoint()
    trackingStartOrdinal = trackingCheckpoint.ordinal

    val playerNames = game.actors.filterIsInstance<Player>().map { it.className }.toSet()
    val unknownPlayers = projectCardArrivalOrder.keys - playerNames
    check(unknownPlayers.isEmpty()) {
      "card arrivals supplied for unknown Players: $unknownPlayers"
    }
    val repeatedCards =
        projectCardArrivalOrder.values
            .flatten()
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
    check(repeatedCards.isEmpty()) { "cards occur more than once in arrival order: $repeatedCards" }
    arrivalOffsets.putAll(projectCardArrivalOrder.keys.associateWith { 0 })
  }

  /** Assigns sourced identities to this Player's next anonymous project-card selection. */
  protected fun TfmGameplay<*>.expectProjectCards(vararg cardClasses: ClassName) {
    syncCardPlays()
    annotateTransition(player, cardClasses.asList()) { event ->
      event.projectCardLocation(event.change.gaining) in setOf(Selecting(player), Hand(player)) &&
          event.change.removing?.className != PROJECT_CARD
    }
    cardClasses.forEach { cardClass ->
      check(cards.put(cardClass, Selecting(player)) == null) {
        "$cardClass has already left the deck"
      }
    }
  }

  /** Allocates distinct replay-local identities for project cards absent from the source. */
  protected fun unknownProjectCards(count: Int): Array<ClassName> {
    require(count >= 0)
    return Array(count) {
      cn("UnknownCard${nextUnknownProjectCard++.toString().padStart(2, '0')}")
    }
  }

  /** Records a sourced deck exit that the game model omits entirely. */
  protected fun TfmGameplay<*>.discardProjectCardsFromDeck(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards.put(cardClass, Terminal) == null) { "$cardClass has already left the deck" }
    }
  }

  /** Marks the named cards from the current selection as terminal. */
  protected fun TfmGameplay<*>.discardUnselectedProjectCards(vararg cardClasses: ClassName) {
    syncCardPlays()
    discardUnselectedProjectCards(player, cardClasses)
  }

  /** Resolves and identifies an anonymous in-operation selection discard. */
  protected fun OperationScope.discardUnselectedProjectCards(vararg cardClasses: ClassName) {
    require(cardClasses.isNotEmpty())
    if (
        tasks
            .extract { it }
            .any { task ->
              task.instruction.toString().let { text ->
                text.startsWith("-") && "ProjectCard" in text && "Selecting" in text
              }
            }
    ) {
      doTask("-${cardClasses.size} ProjectCard<Selecting>")
    }
    discardUnselectedProjectCards(null, cardClasses)
  }

  private fun discardUnselectedProjectCards(
      expectedPlayer: Player?,
      cardClasses: Array<out ClassName>,
  ) {
    val recentStart = trackingCheckpoint.ordinal
    syncCardPlays()
    val locations = cardClasses.map { cards[it] }
    if (projectCardArrivalOrder.isNotEmpty()) {
      val cardsThatNeverArrived =
          cardClasses.zip(locations).filter { (_, location) -> location == null }.map { it.first }
      check(cardsThatNeverArrived.isEmpty()) {
        "cannot discard cards that never arrived: $cardsThatNeverArrived"
      }
    }
    val selectingPlayers = locations.filterIsInstance<Selecting>().map { it.player }.toSet()
    val inferredPlayer = expectedPlayer ?: selectingPlayers.singleOrNull()
    val traceBack = projectCardArrivalOrder.isEmpty()
    cardClasses.zip(locations).forEach { (cardClass, location) ->
      when (location) {
        null -> Unit // Best-effort replay tests may still name a previously anonymous rejection.
        is Selecting ->
            check(expectedPlayer == null || location.player == expectedPlayer) {
              "$cardClass belongs to ${location.player}'s selection"
            }
        is Hand ->
            check(expectedPlayer == null || location.player == expectedPlayer) {
              "$cardClass belongs to ${location.player}'s hand"
            }
        else -> error("$cardClass is not unselected: $location")
      }
    }
    if (inferredPlayer != null) {
      annotateTransition(inferredPlayer, cardClasses.asList(), traceBack = traceBack) { event ->
        event.projectCardLocation(event.change.removing) in
            setOf(Selecting(inferredPlayer), Hand(inferredPlayer)) &&
            event.change.gaining?.className != PROJECT_CARD
      }
    } else {
      annotateRecentTransition(
          cardClasses.asList(),
          recentStart,
          traceBack = traceBack,
      ) { event ->
        event.change.removing.isProjectCardAt(SELECTING) &&
            event.change.gaining?.className != PROJECT_CARD
      }
    }
    cardClasses.forEach { cardClass ->
      when (val location = cards[cardClass]) {
        null -> cards[cardClass] = Terminal
        is Selecting,
        is Hand -> cards[cardClass] = Terminal
        else -> error("unexpected validated location for $cardClass: $location")
      }
    }
    if (inferredPlayer != null) {
      resolveKnownSelection(inferredPlayer)
      resolveKnownHandSelectionCycle(inferredPlayer)
    }
  }

  protected fun TfmGameplay<*>.draw(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses
        .groupBy { cards[it] }
        .forEach { (from, groupedCards) ->
          check(from == null || from is Selecting) { "$groupedCards cannot be drawn from $from" }
          annotateTransition(player, groupedCards, traceBack = true) { event ->
            event.projectCardLocation(event.change.gaining) == Hand(player)
          }
        }
    cardClasses.forEach { cardClass ->
      when (val location = cards[cardClass]) {
        null -> cards[cardClass] = Hand(player)
        is Selecting -> {
          check(location.player == player) {
            "$cardClass belongs to ${location.player}'s selection"
          }
          cards[cardClass] = Hand(player)
        }
        else -> error("$cardClass cannot be drawn from $location")
      }
    }
  }

  protected fun TfmGameplay<*>.returnToHand(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      val location = cards[cardClass]
      check((location is Played || location is CompletedEvent) && location.player == player) {
        "$cardClass is not played by $player: $location"
      }
    }
    annotateTransition(player, cardClasses.asList()) { event ->
      event.projectCardLocation(event.change.gaining) == Hand(player)
    }
    cardClasses.forEach { cardClass -> cards[cardClass] = Hand(player) }
  }

  protected fun TfmGameplay<*>.draw(vararg cards: PetsClass<CardFront<*, *>>) {
    draw(*cards.map { it.className }.toTypedArray())
  }

  protected fun TfmGameplay<*>.buyCards(vararg cardClasses: ClassName): TaskResult {
    val result = buyCards(cardClasses.size)
    draw(*cardClasses)
    return result
  }

  protected fun TfmGameplay<*>.buyCards(vararg cards: PetsClass<CardFront<*, *>>): TaskResult =
      buyCards(*cards.map { it.className }.toTypedArray())

  protected fun TfmGameplay<*>.discard(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards[cardClass] == Hand(player)) {
        "$cardClass should be in ${player}'s hand, but is at ${cards[cardClass]}"
      }
    }
    annotateTransition(player, cardClasses.asList()) { event ->
      event.projectCardLocation(event.change.removing) == Hand(player)
    }
    cardClasses.forEach { cardClass -> move(cardClass, Hand(player), Terminal) }
  }

  protected fun TfmGameplay<*>.discard(vararg cards: PetsClass<CardFront<*, *>>) {
    discard(*cards.map { it.className }.toTypedArray())
  }

  protected fun TfmGameplay<*>.sellPatents(vararg cardClasses: ClassName): TaskResult {
    return stdProject("SellPatentsProject") {
      doTask("${cardClasses.size} MC FROM ProjectCard<Hand>!")
      discard(*cardClasses)
    }
  }

  protected fun TfmGameplay<*>.sellPatents(vararg cards: PetsClass<CardFront<*, *>>): TaskResult =
      sellPatents(*cards.map { it.className }.toTypedArray())

  protected fun assertCardTrackingComplete() {
    syncCardPlays()
    applyPendingAnnotations(fromStart = true)
    check(pendingAnnotations.isEmpty()) {
      "card names left without matching events: ${pendingAnnotations.map { it.cardClasses }}"
    }
    check(cards.values.none { it is Selecting }) {
      "cards left in selections: ${cards.filterValues { it is Selecting }}"
    }
    val unusedArrivals =
        projectCardArrivalOrder
            .mapValues { (player, arrivals) ->
              arrivals.drop(arrivalOffsets.getValue(player))
            }
            .filterValues { it.isNotEmpty() }
    check(unusedArrivals.isEmpty()) { "unused project-card arrivals: $unusedArrivals" }
    assertHandSizesMatch()
    if (requireEveryProjectCardChangeNamed) {
      val unnamedEvents = projectCardEvents.filterNot { it.hasCompleteCardNote() }
      check(unnamedEvents.isEmpty()) {
        "project-card events without every card name: " +
            unnamedEvents.joinToString { "${it.ordinal}: ${it.change}" }
      }
    }
  }

  protected fun checkHandSizes() {
    syncCardPlays()
    assertHandSizesMatch()
  }

  private fun assertHandSizesMatch() {
    game.actors.filterIsInstance<Player>().forEach { player ->
      game.testTfm(player).count("ProjectCard<Hand>") shouldBe
          cards.values.count { it == Hand(player) }
    }
  }

  protected val TfmGameplay<*>.cardsHand: Set<ClassName>
    get() {
      syncCardPlays()
      return cards.filterValues { it == Hand(player) }.keys
    }

  private fun syncCardPlays() {
    val current = game.timeline.checkpoint()
    val syncStart = trackingCheckpoint.ordinal
    check(current.ordinal >= trackingCheckpoint.ordinal) {
      "card tracking crossed an unannounced timeline rollback"
    }
    game.events.entriesSince(trackingCheckpoint).filterIsInstance<ChangeEvent>().forEach { event ->
      if (event.involvesProjectCard()) {
        projectCardEvents += event
        val player =
            event.projectCardPlayer(event.change.gaining)
                ?: event.projectCardPlayer(event.change.removing)
        if (player != null) recentProjectCardStarts[player] = syncStart
      }
      observeCardPlay(event)
    }
    trackingCheckpoint = current
    resolveFullyKeptSelections()
    applyPendingAnnotations()
  }

  private fun observeProjectCardArrival(event: ChangeEvent) {
    val location = event.projectCardLocation(event.change.gaining) ?: return
    if (event.change.removing?.className in setOf(PROJECT_CARD, PLAYED_EVENT)) return
    val player = checkNotNull(location.player)
    val arrivals = projectCardArrivalOrder[player.className] ?: return
    val offset = arrivalOffsets.getValue(player.className)
    val end = offset + event.change.count
    check(end <= arrivals.size) {
      "${player.className}'s project-card arrival order has ${arrivals.size - offset} cards left, " +
          "but event ${event.ordinal} gains ${event.change.count}"
    }
    val arrivingCards = arrivals.subList(offset, end)
    arrivingCards.forEach { cardClass ->
      check(cards.put(cardClass, location) == null) { "$cardClass has already left the deck" }
    }
    event.noteCards(arrivingCards)
    arrivalOffsets[player.className] = end
  }

  private fun resolveFullyKeptSelections() {
    projectCardArrivalOrder.keys.forEach { playerName ->
      val player = game.actors.filterIsInstance<Player>().single { it.className == playerName }
      val selectingCards = cards.filterValues { it == Selecting(player) }.keys.toList()
      if (selectingCards.isEmpty()) return@forEach
      val selected =
          selectEvents(selectingCards.size, trackingStartOrdinal) { event ->
            event.projectCardLocation(event.change.removing) == Selecting(player) &&
                event.projectCardLocation(event.change.gaining) == Hand(player)
          } ?: return@forEach
      annotateSelectedEvents(selected, selectingCards)
      selectingCards.forEach { cards[it] = Hand(player) }
    }
  }

  private fun resolveKnownSelection(player: Player) {
    val selectingCards = cards.filterValues { it == Selecting(player) }.keys.toList()
    if (selectingCards.isEmpty()) return
    val selected =
        checkNotNull(
            selectEvents(selectingCards.size, trackingStartOrdinal) { event ->
              event.projectCardLocation(event.change.removing) == Selecting(player) &&
                  event.projectCardLocation(event.change.gaining) == Hand(player)
            }
        ) {
          "no project-card selection event retains $selectingCards for $player"
        }
    annotateSelectedEvents(selected, selectingCards)
    selectingCards.forEach { cards[it] = Hand(player) }
  }

  private fun resolveKnownHandSelectionCycle(player: Player) {
    val handCards = cards.filterValues { it == Hand(player) }.keys.toList()
    if (handCards.isEmpty()) return
    val movedToSelecting =
        selectEvents(handCards.size, trackingStartOrdinal) { event ->
          event.projectCardLocation(event.change.removing) == Hand(player) &&
              event.projectCardLocation(event.change.gaining) == Selecting(player)
        } ?: return
    annotateSelectedEvents(movedToSelecting, handCards)
    handCards.forEach { cards[it] = Selecting(player) }
    val returnedToHand =
        selectEvents(handCards.size, movedToSelecting.maxOf { it.event.ordinal } + 1) { event ->
          event.projectCardLocation(event.change.removing) == Selecting(player) &&
              event.projectCardLocation(event.change.gaining) == Hand(player)
        } ?: return
    annotateSelectedEvents(returnedToHand, handCards)
    handCards.forEach { cards[it] = Hand(player) }
  }

  private fun observeCardPlay(event: ChangeEvent) {
    observeProjectCardArrival(event)
    observeKnownProjectCardMove(event)
    val gaining = event.change.gaining
    val removing = event.change.removing
    when {
      gaining?.className == PLAYED_EVENT -> {
        val cardClass = checkNotNull(gaining.trackedCardClass())
        val player = cards.getValue(cardClass).player
        checkNotNull(player) { "$cardClass has no Player before becoming a played event" }
        cards[cardClass] = CompletedEvent(player)
      }
      removing.isProjectCardAt(HAND) && gaining?.className != null -> {
        val cardClass = gaining.className
        val location = cards[cardClass] ?: return
        val player = event.playerOwner(checkNotNull(removing))
        check(location == Hand(player)) { "$player played $cardClass from $location" }
        event.noteCards(listOf(cardClass))
        cards[cardClass] = Played(player)
      }
    }
  }

  private fun observeKnownProjectCardMove(event: ChangeEvent) {
    val from = event.projectCardLocation(event.change.removing) ?: return
    val to = event.projectCardLocation(event.change.gaining) ?: return
    val movingCards = cards.filterValues { it == from }.keys.toList()
    if (movingCards.size != event.change.count) return
    event.noteCards(movingCards)
    movingCards.forEach { cards[it] = to }
  }

  private fun annotateTransition(
      player: Player,
      cardClasses: List<ClassName>,
      traceBack: Boolean = false,
      matches: (ChangeEvent) -> Boolean,
  ) {
    val earliestOrdinal = recentProjectCardStarts[player] ?: trackingStartOrdinal
    val annotation = PendingAnnotation(cardClasses, earliestOrdinal, traceBack, matches)
    if (!applyAnnotation(annotation)) {
      pendingAnnotations += annotation.copy(earliestOrdinal = trackingCheckpoint.ordinal)
    }
  }

  private fun annotateRecentTransition(
      cardClasses: List<ClassName>,
      earliestOrdinal: Int,
      traceBack: Boolean,
      matches: (ChangeEvent) -> Boolean,
  ) {
    val annotation = PendingAnnotation(cardClasses, earliestOrdinal, traceBack, matches)
    if (!applyAnnotation(annotation)) {
      pendingAnnotations += annotation.copy(earliestOrdinal = trackingCheckpoint.ordinal)
    }
  }

  private fun applyPendingAnnotations(fromStart: Boolean = false) {
    val iterator = pendingAnnotations.iterator()
    while (iterator.hasNext()) {
      val annotation = iterator.next()
      val candidate =
          if (fromStart) annotation.copy(earliestOrdinal = trackingStartOrdinal) else annotation
      if (applyAnnotation(candidate)) iterator.remove()
    }
  }

  private fun applyAnnotation(annotation: PendingAnnotation): Boolean {
    if (annotationAlreadyApplied(annotation)) return true
    val selected =
        selectEvents(
            annotation.cardClasses.size,
            annotation.earliestOrdinal,
            matches = annotation.matches,
        ) ?: return false
    annotateSelectedEvents(selected, annotation.cardClasses)

    if (!annotation.traceBack) return true
    var successors = selected
    while (true) {
      val priorLocation =
          successors
              .mapNotNull { allocation ->
                allocation.event.projectCardLocation(allocation.event.change.removing)
              }
              .distinct()
              .singleOrNull() ?: return true
      val beforeOrdinal = successors.minOf { allocation -> allocation.event.ordinal }
      val predecessors =
          selectEvents(
              annotation.cardClasses.size,
              trackingStartOrdinal,
              beforeOrdinal,
          ) { event ->
            event.projectCardLocation(event.change.gaining) == priorLocation
          } ?: return true
      annotateSelectedEvents(predecessors, annotation.cardClasses)
      successors = predecessors
    }
  }

  private fun annotationAlreadyApplied(annotation: PendingAnnotation): Boolean =
      projectCardEvents
          .filter { event ->
            event.ordinal >= annotation.earliestOrdinal && annotation.matches(event)
          }
          .groupBy { event ->
            ProjectCardTransition(
                event.projectCardLocation(event.change.removing),
                event.projectCardLocation(event.change.gaining),
            )
          }
          .values
          .any { events ->
            events
                .flatMap { event -> eventCards[event].orEmpty() }
                .containsAll(annotation.cardClasses)
          }

  private fun selectEvents(
      cardCount: Int,
      earliestOrdinal: Int,
      beforeOrdinal: Int = Int.MAX_VALUE,
      matches: (ChangeEvent) -> Boolean,
  ): List<EventAllocation>? {
    val matching = projectCardEvents.filter { event ->
      event.ordinal >= earliestOrdinal &&
          event.ordinal < beforeOrdinal &&
          event.remainingCardCapacity > 0 &&
          matches(event)
    }
    val transitionGroups =
        matching
            .groupBy { event ->
              ProjectCardTransition(
                  event.projectCardLocation(event.change.removing),
                  event.projectCardLocation(event.change.gaining),
              )
            }
            .values
            .sortedByDescending { events -> events.maxOf { it.ordinal } }
    val group =
        transitionGroups.firstOrNull { events ->
          events.sumOf { it.remainingCardCapacity } >= cardCount
        } ?: return null
    val selected = mutableListOf<EventAllocation>()
    var remaining = cardCount
    for (event in group.asReversed()) {
      val available = event.remainingCardCapacity
      val assigned = minOf(available, remaining)
      selected += EventAllocation(event, assigned)
      remaining -= assigned
      if (remaining == 0) break
    }
    if (remaining != 0) return null
    return selected.asReversed()
  }

  private fun annotateSelectedEvents(
      selected: List<EventAllocation>,
      cardClasses: List<ClassName>,
  ) {
    var cardIndex = 0
    selected.forEach { (event, count) ->
      val eventCards = cardClasses.slice(cardIndex until cardIndex + count)
      event.noteCards(eventCards)
      cardIndex += count
    }
  }

  private fun ChangeEvent.projectCardLocation(expression: Expression?): CardLocation? {
    if (expression?.className != PROJECT_CARD) return null
    val player = playerOwner(expression)
    return when {
      expression.isProjectCardAt(HAND) -> Hand(player)
      expression.isProjectCardAt(SELECTING) -> Selecting(player)
      else -> null
    }
  }

  private fun ChangeEvent.projectCardPlayer(expression: Expression?): Player? =
      expression?.takeIf { it.className == PROJECT_CARD }?.let { playerOwner(it) }

  private fun ChangeEvent.involvesProjectCard(): Boolean =
      change.gaining?.className == PROJECT_CARD || change.removing?.className == PROJECT_CARD

  private fun ChangeEvent.noteCards(cardClasses: List<ClassName>) {
    val notedCards = eventCards.getOrPut(this) { mutableListOf() }
    val previousCardNote = trackedCardNote
    cardClasses.filterNotTo(notedCards) { it in notedCards }
    check(notedCards.size <= change.count) { "$notedCards exceed project-card count in $this" }
    val cardNote = checkNotNull(trackedCardNote)
    val currentNotes = notes
    notes =
        when {
          previousCardNote == null -> listOfNotNull(currentNotes, cardNote).joinToString("\n")
          currentNotes == null -> cardNote
          currentNotes.lineSequence().any { it == previousCardNote } ->
              currentNotes.lineSequence().joinToString("\n") {
                if (it == previousCardNote) cardNote else it
              }
          else -> "$currentNotes\n$cardNote"
        }
  }

  private fun ChangeEvent.hasCompleteCardNote(): Boolean =
      remainingCardCapacity == 0 &&
          trackedCardNote?.let { expected -> notes?.lineSequence()?.any { it == expected } } == true

  private val ChangeEvent.trackedCardNote: String?
    get() = eventCards[this]?.takeIf { it.isNotEmpty() }?.let { "Cards: ${it.joinToString()}" }

  private val ChangeEvent.remainingCardCapacity: Int
    get() = change.count - eventCards[this].orEmpty().size

  private fun move(cardClass: ClassName, from: CardLocation, to: CardLocation) {
    check(cards[cardClass] == from) {
      "$cardClass should be at $from, but is at ${cards[cardClass]}"
    }
    cards[cardClass] = to
  }

  private fun ChangeEvent.playerOwner(expression: Expression): Player =
      checkNotNull(
          expression.toComponent(game.reader).owner?.className?.let { ownerName ->
            game.actors.filterIsInstance<Player>().singleOrNull { it.className == ownerName }
          }
      ) {
        "$expression changed without a Player owner in $this"
      }

  private val TfmGameplay<*>.player: Player
    get() = actor as Player

  private fun Expression?.isProjectCardAt(area: ClassName): Boolean =
      this?.className == PROJECT_CARD && arguments.any { it.className == area }

  private fun Expression.trackedCardClass(): ClassName? =
      descendantsOfType<ClassName>().firstOrNull { it in cards }

  private sealed interface CardLocation {
    val player: Player?
  }

  private data class Selecting(override val player: Player) : CardLocation

  private data class Hand(override val player: Player) : CardLocation

  private data class Played(override val player: Player) : CardLocation

  private data class CompletedEvent(override val player: Player) : CardLocation

  private data object Terminal : CardLocation {
    override val player: Player? = null
  }

  private data class EventAllocation(val event: ChangeEvent, val count: Int)

  private data class ProjectCardTransition(val from: CardLocation?, val to: CardLocation?)

  private data class PendingAnnotation(
      val cardClasses: List<ClassName>,
      val earliestOrdinal: Int,
      val traceBack: Boolean,
      val matches: (ChangeEvent) -> Boolean,
  )

  private companion object {
    val PROJECT_CARD: ClassName = cn("ProjectCard")
    val HAND: ClassName = cn("Hand")
    val SELECTING: ClassName = cn("Selecting")
    val PLAYED_EVENT: ClassName = cn("PlayedEvent")
  }
}
