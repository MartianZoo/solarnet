package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class CardTrackingFullGameTest(
    private val requireEveryProjectCardChangeNamed: Boolean = false,
) : AbstractFullGameTest() {
  /** Source-known card identities in the order they enter each Player's modeled hand. */
  protected open val projectCardArrivalOrder: Map<ClassName, List<ClassName>> = emptyMap()

  private val cards = linkedMapOf<ClassName, CardState>()
  private val arrivalOffsets = mutableMapOf<ClassName, Int>()
  private val projectCardEvents = mutableListOf<ChangeEvent>()
  private val eventCards = mutableMapOf<ChangeEvent, MutableList<ClassName>>()
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
          event.change.gaining?.className == PROJECT_CARD &&
          event.projectCardPlayer() == player &&
          cardClass in eventCards[event].orEmpty()
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
      if (event.involvesProjectCard()) {
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
    val gaining = event.change.gaining
    val removing = event.change.removing
    when {
      gaining?.className == PROJECT_CARD && removing?.className == PLAYED_EVENT -> {
        val cardClass = checkNotNull(removing.trackedCardClass())
        val player = event.playerOwner(gaining)
        cards[cardClass] = Hand(player)
        event.noteCards(listOf(cardClass))
      }
      gaining?.className == PROJECT_CARD && removing?.className != PROJECT_CARD ->
          observeProjectCardArrival(event)
      removing?.className == PROJECT_CARD && gaining?.className != null -> {
        val cardClass = gaining.className
        val player = event.playerOwner(removing)
        val state = cards[cardClass] ?: return
        check(state == Hand(player)) { "$player played $cardClass from $state" }
        cards[cardClass] = Played(player)
        event.noteCards(listOf(cardClass))
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
    event.noteCards(arrivingCards)
    arrivalOffsets[player.className] = end
  }

  private fun annotateProjectCardChange(
      player: Player,
      cardClasses: List<ClassName>,
      gaining: Boolean,
  ) {
    val matches: (ChangeEvent) -> Boolean = { event ->
      event.projectCardPlayer() == player &&
          if (gaining) {
            event.change.gaining?.className == PROJECT_CARD &&
                event.change.removing?.className != PROJECT_CARD
          } else {
            event.change.removing?.className == PROJECT_CARD &&
                event.change.gaining?.className != PROJECT_CARD
          }
    }
    val annotation =
        PendingAnnotation(
            cardClasses,
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
        selectEvents(annotation.cardClasses.size, annotation.earliestOrdinal, annotation.matches)
            ?: return false
    annotateSelectedEvents(selected, annotation.cardClasses)
    return true
  }

  private fun selectEvents(
      cardCount: Int,
      earliestOrdinal: Int,
      matches: (ChangeEvent) -> Boolean,
  ): List<EventAllocation>? {
    val matching =
        projectCardEvents
            .filter { event ->
              event.ordinal >= earliestOrdinal && event.remainingCardCapacity > 0 && matches(event)
            }
            .asReversed()
    val selected = mutableListOf<EventAllocation>()
    var remaining = cardCount
    for (event in matching) {
      val assigned = minOf(event.remainingCardCapacity, remaining)
      selected += EventAllocation(event, assigned)
      remaining -= assigned
      if (remaining == 0) break
    }
    return if (remaining == 0) selected.asReversed() else null
  }

  private fun annotateSelectedEvents(
      selected: List<EventAllocation>,
      cardClasses: List<ClassName>,
  ) {
    var cardIndex = 0
    selected.forEach { (event, count) ->
      event.noteCards(cardClasses.slice(cardIndex until cardIndex + count))
      cardIndex += count
    }
  }

  private fun ChangeEvent.projectCardPlayer(): Player? {
    val component =
        listOfNotNull(change.gaining, change.removing).firstOrNull {
          it.className == PROJECT_CARD
        } ?: return null
    return playerOwner(component)
  }

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

  private fun ChangeEvent.playerOwner(component: Component): Player =
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
      val earliestOrdinal: Int,
      val matches: (ChangeEvent) -> Boolean,
  )

  private companion object {
    val PROJECT_CARD: ClassName = cn("ProjectCard")
    val PLAYED_EVENT: ClassName = cn("PlayedEvent")
  }
}
