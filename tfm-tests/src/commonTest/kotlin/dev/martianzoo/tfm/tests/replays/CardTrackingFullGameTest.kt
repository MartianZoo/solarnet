package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class CardTrackingFullGameTest : AbstractFullGameTest() {
  private val cards = linkedMapOf<ClassName, CardLocation>()
  private lateinit var trackingCheckpoint: Checkpoint

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()
    cards.clear()
    trackingCheckpoint = game.timeline.checkpoint()
  }

  /** Assigns sourced identities to this Player's next anonymous project-card selection. */
  protected fun TfmGameplay.expectProjectCards(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards.put(cardClass, Selecting(player)) == null) {
        "$cardClass has already left the deck"
      }
    }
  }

  /** Records a sourced deck exit that the game model omits entirely. */
  protected fun TfmGameplay.discardProjectCardsFromDeck(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      check(cards.put(cardClass, Terminal) == null) { "$cardClass has already left the deck" }
    }
  }

  /** Marks the named cards from the current selection as terminal. */
  protected fun TfmGameplay.discardUnselectedProjectCards(vararg cardClasses: ClassName) {
    syncCardPlays()
    discardUnselectedProjectCards(player, cardClasses)
  }

  /** Resolves and identifies an anonymous in-operation selection discard. */
  protected fun OperationBody.discardUnselectedProjectCards(vararg cardClasses: ClassName) {
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
    cardClasses.forEach { cardClass ->
      when (val location = cards[cardClass]) {
        null -> cards[cardClass] = Terminal
        is Selecting -> {
          check(expectedPlayer == null || location.player == expectedPlayer) {
            "$cardClass belongs to ${location.player}'s selection"
          }
          cards[cardClass] = Terminal
        }
        else -> error("$cardClass is not unselected: $location")
      }
    }
  }

  protected fun TfmGameplay.draw(vararg cardClasses: ClassName) {
    syncCardPlays()
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

  protected fun TfmGameplay.returnToHand(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass ->
      val location = cards[cardClass]
      check((location is Played || location is EventPile) && location.player == player) {
        "$cardClass is not played by $player: $location"
      }
      cards[cardClass] = Hand(player)
    }
  }

  protected fun TfmGameplay.buyCards(vararg cardClasses: ClassName): TaskResult {
    val result = buyCards(cardClasses.size)
    draw(*cardClasses)
    return result
  }

  protected fun TfmGameplay.discard(vararg cardClasses: ClassName) {
    syncCardPlays()
    cardClasses.forEach { cardClass -> move(cardClass, Hand(player), Terminal) }
  }

  protected fun TfmGameplay.sellPatents(vararg cardClasses: ClassName): TaskResult {
    return stdAction("SellPatents") {
      doTask("${cardClasses.size} MC FROM ProjectCard<Hand>!")
      discard(*cardClasses)
    }
  }

  protected fun assertCardTrackingComplete() {
    syncCardPlays()
    check(cards.values.none { it is Selecting }) {
      "cards left in selections: ${cards.filterValues { it is Selecting }}"
    }
  }

  protected fun checkHandSizes() {
    syncCardPlays()
    game.actors.filterIsInstance<Player>().forEach { player ->
      game.tfm(player).count("ProjectCard<Hand>") shouldBe cards.values.count { it == Hand(player) }
    }
  }

  protected val TfmGameplay.cardsInHand: Set<ClassName>
    get() {
      syncCardPlays()
      return cards.filterValues { it == Hand(player) }.keys
    }

  private fun syncCardPlays() {
    val current = game.timeline.checkpoint()
    check(current.ordinal >= trackingCheckpoint.ordinal) {
      "card tracking crossed an unannounced timeline rollback"
    }
    game.events
        .entriesSince(trackingCheckpoint)
        .filterIsInstance<ChangeEvent>()
        .forEach(::observeCardPlay)
    trackingCheckpoint = current
  }

  private fun observeCardPlay(event: ChangeEvent) {
    val gaining = event.change.gaining
    val removing = event.change.removing
    when {
      gaining?.className == PLAYED_EVENT -> {
        val cardClass = checkNotNull(gaining.trackedCardClass())
        val player = cards.getValue(cardClass).player
        checkNotNull(player) { "$cardClass has no Player before entering the event pile" }
        cards[cardClass] = EventPile(player)
      }
      removing.isProjectCardAt(HAND) && gaining?.className != null -> {
        val cardClass = gaining.className
        val location = cards[cardClass] ?: return
        val player = event.playerOwner(checkNotNull(removing))
        check(location == Hand(player)) { "$player played $cardClass from $location" }
        cards[cardClass] = Played(player)
      }
    }
  }

  private fun move(cardClass: ClassName, from: CardLocation, to: CardLocation) {
    check(cards[cardClass] == from) {
      "$cardClass should be at $from, but is at ${cards[cardClass]}"
    }
    cards[cardClass] = to
  }

  private fun ChangeEvent.playerOwner(expression: Expression): Player =
      checkNotNull(
          expression.toComponent(game.reader).owner?.className?.let(Player::fromClassNameOrNull)
      ) {
        "$expression changed without a Player owner in $this"
      }

  private val TfmGameplay.player: Player
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

  private data class EventPile(override val player: Player) : CardLocation

  private data object Terminal : CardLocation {
    override val player: Player? = null
  }

  private companion object {
    val PROJECT_CARD: ClassName = cn("ProjectCard")
    val HAND: ClassName = cn("Hand")
    val PLAYED_EVENT: ClassName = cn("PlayedEvent")
  }
}
