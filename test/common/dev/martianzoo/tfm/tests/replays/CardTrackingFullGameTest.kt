package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.engine.TfmGameplay
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class CardTrackingFullGameTest : AbstractFullGameTest() {
  /** Source-known card identities in the order they enter each Player's modeled hand. */
  protected open val projectCardArrivalOrder: Map<ClassName, List<ClassName>> = emptyMap()

  private val cards = linkedMapOf<ClassName, CardState>()
  private val arrivalOffsets = mutableMapOf<ClassName, Int>()
  private val projectCardEvents = mutableListOf<ChangeEvent>()
  private val eventCards = mutableMapOf<Pair<ChangeEvent, Boolean>, MutableList<ClassName>>()
  private val recentProjectCardStarts = mutableMapOf<Player, Int>()
  private val pendingAnnotations = mutableListOf<PendingAnnotation>()
  private var nextUnknownProjectCard = 1
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

  /** Allocates distinct replay-local identities for project cards absent from the source. */
  protected fun unknownProjectCards(count: Int): Array<ClassName> {
    require(count >= 0)
    return Array(count) {
      cn("UnknownCard${nextUnknownProjectCard++.toString().padStart(2, '0')}")
    }
  }

  protected fun TfmGameplay.draw(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards.put(cardClass, Hand(player)) == null) { "$cardClass has already left the deck" }
    }
    annotateProjectCardChange(player, cardClasses.asList(), gaining = true)
  }

  protected fun TfmGameplay.returnToHand(vararg cardClasses: ClassName) {
    syncCardPlays()
    val cardsNeedingAnnotation = mutableListOf<ClassName>()
    cardClasses.forEach { cardClass ->
      val state = cards[cardClass]
      check(
          (state is Hand || state is Played || state is CompletedEvent) && state.player == player
      ) {
        "$cardClass is not played by $player: $state"
      }
      if (state !is Hand) {
        cards[cardClass] = Hand(player)
      }
      if (!cardWasNamedInRecentArrival(player, cardClass)) cardsNeedingAnnotation += cardClass
    }
    if (cardsNeedingAnnotation.isNotEmpty()) {
      annotateProjectCardChange(player, cardsNeedingAnnotation, gaining = true)
    }
  }

  private fun cardWasNamedInRecentArrival(player: Player, cardClass: ClassName): Boolean {
    val earliestOrdinal = recentProjectCardStarts[player] ?: trackingStartOrdinal
    return projectCardEvents.any { event ->
      event.ordinal >= earliestOrdinal &&
          event.change.gaining.isProjectCardAt(HAND) &&
          event.projectCardPlayer() == player &&
          cardClass in eventCards[event to true].orEmpty()
    }
  }

  protected fun TfmGameplay.buyCards(vararg cardClasses: ClassName): TaskResult {
    val result = buyCards(cardClasses.size)
    draw(*cardClasses)
    return result
  }

  protected fun TfmGameplay.discard(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards[cardClass] == Hand(player)) {
        "$cardClass should be in ${player}'s hand, but is at ${cards[cardClass]}"
      }
      cards[cardClass] = Terminal
    }
    annotateProjectCardChange(player, cardClasses.asList(), gaining = false)
  }

  /** Names both movements on the acting Player, like the replay's draw and discard annotations. */
  protected fun TfmGameplay.nameRevealedCards(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards[cardClass] == Hand(player)) {
        "$cardClass should be back in ${player}'s hand, but is at ${cards[cardClass]}"
      }
    }
    annotateProjectCardChange(player, cardClasses.asList(), gaining = false) {
      it.movesBetweenHandAndReveal()
    }
    annotateProjectCardChange(player, cardClasses.asList(), gaining = true) {
      it.movesBetweenHandAndReveal()
    }
  }

  protected fun TfmGameplay.sellPatents(vararg cardClasses: ClassName): TaskResult {
    return stdProject("SellPatentsProject") {
      doTask("${cardClasses.size} MC FROM ProjectCard!")
      discard(*cardClasses)
    }
  }

  protected fun assertCardTrackingComplete() {
    syncCardPlays()
    applyPendingAnnotations(fromStart = true)
    check(pendingAnnotations.isEmpty()) {
      "card names left without matching events: ${pendingAnnotations.map { it.cardClasses }}"
    }
    val unusedArrivals =
        projectCardArrivalOrder
            .mapValues { (player, arrivals) -> arrivals.drop(arrivalOffsets.getValue(player)) }
            .filterValues { it.isNotEmpty() }
    check(unusedArrivals.isEmpty()) { "unused project-card arrivals: $unusedArrivals" }
    assertHandSizesMatch()
    val unnamedEvents = projectCardEvents.filterNot { it.hasCompleteCardNote() }
    check(unnamedEvents.isEmpty()) {
      "project-card events without every card name: " +
          unnamedEvents.joinToString { "${it.ordinal}: ${it.change}" }
    }
    assertCardCriteria()
  }

  /** Names the deck card flipped by this action without adding it to the hand ledger. */
  protected fun nameFlippedCard(result: TaskResult, cardClass: ClassName) {
    val reveal =
        result.changes.single {
          it.change.gaining.isProjectCardAt(REVEALED) && !it.movesBetweenHandAndReveal()
        }
    check(eventCards[reveal to true].isNullOrEmpty()) { "flip already named: $reveal" }
    reveal.noteCards(listOf(cardClass), gaining = true)
  }

  private fun assertCardCriteria() {
    val events = game.events.entriesSince(Checkpoint(trackingStartOrdinal))
    val changes = events.filterIsInstance<ChangeEvent>()
    changes.forEachIndexed { index, event ->
      val context = event.cause?.context ?: return@forEachIndexed
      if (
          event.change.gaining.isProjectCardAt(HAND) &&
              context.className in setOf(cn("SearchForCard"), cn("TakeSelectedCard"))
      ) {
        val filter = cardFilter(context)
        eventCards[event to true].orEmpty().forEach { card ->
          check(cardMatchesFilter(card, filter)) {
            "$card does not satisfy $filter at event ${event.ordinal}"
          }
        }
      }
      if (!event.change.gaining.isProjectCardAt(REVEALED) || event.movesBetweenHandAndReveal()) {
        return@forEachIndexed
      }
      val player = event.projectCardPlayer()
      val following = changes.drop(index + 1)
      val end = following.indexOfFirst {
        it.projectCardPlayer() == player && it.change.removing.isProjectCardAt(REVEALED)
      }
      check(end >= 0) { "flipped card was not discarded: $event" }
      // Read the actual offered choice, so even a declined claim carries its Pets criterion.
      val claim =
          events
              .filterIsInstance<TaskAddedEvent>()
              .filter {
                it.ordinal > event.ordinal &&
                    it.ordinal < following[end].ordinal &&
                    it.task.assignee == player
              }
              .flatMap { it.task.instruction.descendantsOfType<Expression>() }
              .filter { it.className == cn("ClaimCardReward") }
              .distinct()
              .single()
      val filter = cardFilter(claim)
      val claimed =
          following.take(end).any {
            it.change.gaining?.className == claim.className &&
                it.owningPlayer(checkNotNull(it.change.gaining)) == player
          }
      val names = eventCards[event to true].orEmpty()
      check(!claimed || names.size == event.change.count) {
        "$claim requires the flipped card's identity at event ${event.ordinal}"
      }
      names.forEach { card ->
        check(cardMatchesFilter(card, filter) == claimed) {
          "$card ${if (claimed) "does not satisfy" else "satisfies"} $filter, but claim=$claimed at event ${event.ordinal}"
        }
      }
    }
  }

  private fun cardFilter(expression: Expression): Expression =
      expression.arguments.single {
        game.classTable
            .getClass(it.className)
            .isSubtypeOf(game.classTable.getClass(cn("CardFilter")))
      }

  private fun cardMatchesFilter(cardClass: ClassName, filter: Expression): Boolean {
    val card = catalog.card(cardClass)
    return when (filter.className) {
      cn("TagFilter") -> {
        val tag =
            catalog.classTable.getClass(filter.arguments.single().arguments.single().className)
        cardTags(card).elements.any { catalog.classTable.getClass(it).isSubtypeOf(tag) }
      }
      cn("NoTagsFilter") -> cardTags(card).isEmpty()
      cn("ReferenceFilter") -> {
        val reference =
            catalog.classTable.getClass(filter.arguments.single().arguments.single().className)
        card.declaration.allNodes.any { node ->
          node.descendantsOfType<ClassName>().any {
            catalog.classTable.findClass(it)?.isSubtypeOf(reference) == true
          }
        }
      }
      else -> error("unrecognized card filter: $filter")
    }
  }

  protected fun checkHandSizes() {
    syncCardPlays()
    assertHandSizesMatch()
  }

  private fun assertHandSizesMatch() {
    game.actors.filterIsInstance<Player>().forEach { player ->
      game.testTfm(player).count("ProjectCard") shouldBe cards.values.count { it == Hand(player) }
    }
  }

  protected val TfmGameplay.cardsHand: Set<ClassName>
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
      if (event.involvesHandCard()) {
        projectCardEvents += event
        val player = event.projectCardPlayer()
        if (player != null) recentProjectCardStarts[player] = syncStart
      }
      observeCardChange(event)
    }
    trackingCheckpoint = current
    applyPendingAnnotations()
  }

  private fun observeCardChange(event: ChangeEvent) {
    // Hand membership is unchanged after a full reveal cycle; its two events still need names.
    if (event.movesBetweenHandAndReveal()) return
    val gaining = event.change.gaining
    val removing = event.change.removing
    when {
      gaining.isProjectCardAt(HAND) && removing?.className == PLAYED_EVENT -> {
        val cardClass = checkNotNull(removing.trackedCardClass())
        val player = event.owningPlayer(checkNotNull(gaining))
        cards[cardClass] = Hand(player)
        event.noteCards(listOf(cardClass), gaining = true)
      }
      gaining.isProjectCardAt(HAND) -> observeProjectCardArrival(event)
      removing.isProjectCardAt(HAND) && gaining?.className != null -> {
        val cardClass = gaining.className
        val player = event.owningPlayer(checkNotNull(removing))
        val state = cards[cardClass] ?: return
        check(state == Hand(player)) { "$player played $cardClass from $state" }
        cards[cardClass] = Played(player)
        event.noteCards(listOf(cardClass), gaining = false)
      }
      gaining?.className == PLAYED_EVENT -> {
        val cardClass = checkNotNull(gaining.trackedCardClass())
        val player = cards.getValue(cardClass).player
        checkNotNull(player) { "$cardClass has no Player before becoming a played event" }
        cards[cardClass] = CompletedEvent(player)
      }
    }
  }

  private fun observeProjectCardArrival(event: ChangeEvent) {
    if (!event.change.gaining.isProjectCardAt(HAND)) return
    val player = event.projectCardPlayer() ?: return
    val arrivals = projectCardArrivalOrder[player.className] ?: return
    val offset = arrivalOffsets.getValue(player.className)
    val end = offset + event.change.count
    check(end <= arrivals.size) {
      "${player.className}'s project-card arrival order has ${arrivals.size - offset} cards left, " +
          "but event ${event.ordinal} gains ${event.change.count}"
    }
    val arrivingCards = arrivals.subList(offset, end)
    arrivingCards.forEach { cardClass ->
      check(cards.put(cardClass, Hand(player)) == null) { "$cardClass has already left the deck" }
    }
    event.noteCards(arrivingCards, gaining = true)
    arrivalOffsets[player.className] = end
  }

  private fun annotateProjectCardChange(
      player: Player,
      cardClasses: List<ClassName>,
      gaining: Boolean,
      additionalMatch: (ChangeEvent) -> Boolean = { !it.movesBetweenHandAndReveal() },
  ) {
    val matches: (ChangeEvent) -> Boolean = { event ->
      event.projectCardPlayer() == player &&
          additionalMatch(event) &&
          if (gaining) event.change.gaining.isProjectCardAt(HAND)
          else event.change.removing.isProjectCardAt(HAND)
    }
    val annotation =
        PendingAnnotation(
            cardClasses,
            gaining,
            recentProjectCardStarts[player] ?: trackingStartOrdinal,
            matches,
        )
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
    val selected =
        selectEvents(
            annotation.cardClasses.size,
            annotation.earliestOrdinal,
            annotation.gaining,
            annotation.matches,
        ) ?: return false
    annotateSelectedEvents(selected, annotation.cardClasses, annotation.gaining)
    return true
  }

  private fun selectEvents(
      cardCount: Int,
      earliestOrdinal: Int,
      gaining: Boolean,
      matches: (ChangeEvent) -> Boolean,
  ): List<EventAllocation>? {
    val matching =
        projectCardEvents
            .filter { event ->
              event.ordinal >= earliestOrdinal &&
                  remainingCardCapacity(event, gaining) > 0 &&
                  matches(event)
            }
            .asReversed()
    val selected = mutableListOf<EventAllocation>()
    var remaining = cardCount
    for (event in matching) {
      val assigned = minOf(remainingCardCapacity(event, gaining), remaining)
      selected += EventAllocation(event, assigned)
      remaining -= assigned
      if (remaining == 0) break
    }
    return if (remaining == 0) selected.asReversed() else null
  }

  private fun annotateSelectedEvents(
      selected: List<EventAllocation>,
      cardClasses: List<ClassName>,
      gaining: Boolean,
  ) {
    var cardIndex = 0
    selected.forEach { (event, count) ->
      event.noteCards(cardClasses.slice(cardIndex until cardIndex + count), gaining)
      cardIndex += count
    }
  }

  private fun ChangeEvent.projectCardPlayer(): Player? {
    val component =
        listOfNotNull(change.gaining, change.removing).firstOrNull {
          it.className == PROJECT_CARD
        } ?: return null
    return owningPlayer(component)
  }

  private fun ChangeEvent.involvesHandCard(): Boolean =
      change.gaining.isProjectCardAt(HAND) || change.removing.isProjectCardAt(HAND)

  private fun ChangeEvent.movesBetweenHandAndReveal(): Boolean =
      (change.removing.isProjectCardAt(HAND) && change.gaining.isProjectCardAt(REVEALED)) ||
          (change.removing.isProjectCardAt(REVEALED) && change.gaining.isProjectCardAt(HAND))

  private fun Component?.isProjectCardAt(location: ClassName): Boolean =
      this?.className == PROJECT_CARD && expressionFull.arguments.any { it.className == location }

  private fun ChangeEvent.noteCards(cardClasses: List<ClassName>, gaining: Boolean) {
    val notedCards = eventCards.getOrPut(this to gaining) { mutableListOf() }
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
      (!change.gaining.isProjectCardAt(HAND) || remainingCardCapacity(this, true) == 0) &&
          (!change.removing.isProjectCardAt(HAND) || remainingCardCapacity(this, false) == 0) &&
          trackedCardNote?.let { expected -> notes?.lineSequence()?.any { it == expected } } == true

  private val ChangeEvent.trackedCardNote: String?
    get() {
      val gained = eventCards[this to true].orEmpty()
      val removed = eventCards[this to false].orEmpty()
      if (gained.isEmpty() && removed.isEmpty()) return null
      return if (gained.isNotEmpty() && removed.isNotEmpty()) {
        "Cards: ${gained.joinToString()} FROM ${removed.joinToString()}"
      } else {
        "Cards: ${(gained + removed).joinToString()}"
      }
    }

  private fun remainingCardCapacity(event: ChangeEvent, gaining: Boolean): Int =
      event.change.count - eventCards[event to gaining].orEmpty().size

  private fun ChangeEvent.owningPlayer(component: Component): Player =
      checkNotNull(
          component.owner?.className?.let { ownerName ->
            game.actors.filterIsInstance<Player>().singleOrNull { it.className == ownerName }
          }
      ) {
        "$component changed without a Player owner in $this"
      }

  private val TfmGameplay.player: Player
    get() = actor as Player

  private fun Component.trackedCardClass(): ClassName? =
      expressionFull.descendantsOfType<ClassName>().firstOrNull { it in cards }

  private sealed interface CardState {
    val player: Player?
  }

  private data class Hand(override val player: Player) : CardState

  private data class Played(override val player: Player) : CardState

  private data class CompletedEvent(override val player: Player) : CardState

  private data object Terminal : CardState {
    override val player: Player? = null
  }

  private data class EventAllocation(val event: ChangeEvent, val count: Int)

  private data class PendingAnnotation(
      val cardClasses: List<ClassName>,
      val gaining: Boolean,
      val earliestOrdinal: Int,
      val matches: (ChangeEvent) -> Boolean,
  )

  private companion object {
    val PROJECT_CARD: ClassName = cn("ProjectCard")
    val PLAYED_EVENT: ClassName = cn("PlayedEvent")
    val HAND: ClassName = cn("Hand")
    val REVEALED: ClassName = cn("Revealed")
  }
}
