package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class CardTrackingFullGameTest : AbstractFullGameTest() {
  private val hands = mutableMapOf<Player, MutableSet<ClassName>>()
  private val seenCards = mutableSetOf<ClassName>()
  private val expectedDraws = mutableMapOf<Player, MutableList<ClassName>>()
  private val expectedDiscards = mutableMapOf<Player, MutableList<ClassName>>()
  private lateinit var trackingCheckpoint: Checkpoint
  private var observerInstalled = false

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()
    hands.clear()
    seenCards.clear()
    expectedDraws.clear()
    expectedDiscards.clear()
    observerInstalled = false
    game.actors.filterIsInstance<Player>().forEach { hands[it] = mutableSetOf() }
    trackingCheckpoint = game.timeline.checkpoint()
  }

  protected fun TfmGameplay.draw(vararg cardClasses: ClassName) {
    installObserver()
    cardClasses.forEach { cardClass ->
      check(seenCards.add(cardClass)) { "$cardClass has already been mentioned" }
      check(hand(player).add(cardClass)) { "$player already has $cardClass in hand" }
    }
    expect(cardClasses, expectedDraws)
  }

  protected fun TfmGameplay.returnToHand(vararg cardClasses: ClassName) {
    installObserver()
    cardClasses.forEach { cardClass ->
      check(cardClass in seenCards) { "$cardClass has not previously been mentioned" }
      check(hands.values.none { cardClass in it }) { "$cardClass is already in a player's hand" }
      check(hand(player).add(cardClass)) { "$player already has $cardClass in hand" }
    }
    expect(cardClasses, expectedDraws)
  }

  protected fun TfmGameplay.buyCards(vararg cardClasses: ClassName): TaskResult {
    draw(*cardClasses)
    return buyCards(cardClasses.size)
  }

  protected fun TfmGameplay.discard(vararg cardClasses: ClassName) {
    installObserver()
    cardClasses.forEach {
      check(it in hand(player) || it in expectedDraws[player].orEmpty()) {
        "$player does not have $it in hand or queued to draw"
      }
    }
    expect(cardClasses, expectedDiscards)
  }

  protected fun TfmGameplay.sellPatents(vararg cardClasses: ClassName): TaskResult {
    return stdAction("SellPatents") {
      doTask("${cardClasses.size} Megacredit FROM ProjectCard<Hand>!")
      discard(*cardClasses)
    }
  }

  protected fun assertCardTrackingComplete() {
    check(expectedDraws.values.all { it.isEmpty() }) { "unconsumed draws: $expectedDraws" }
    check(expectedDiscards.values.all { it.isEmpty() }) { "unconsumed discards: $expectedDiscards" }
  }

  protected fun checkHandSizes() {
    hands.forEach { (player, hand) ->
      game.tfm(player).count("ProjectCard<Hand>") shouldBe hand.size
    }
  }

  protected val TfmGameplay.cardsInHand: Set<ClassName>
    get() = hand(player)

  private fun installObserver() {
    if (observerInstalled) return
    observerInstalled = true
    val previousOnAtomicComplete = game.onAtomicComplete
    game.onAtomicComplete = {
      val changes = game.events.entriesSince(trackingCheckpoint).filterIsInstance<ChangeEvent>()
      trackingCheckpoint = game.timeline.checkpoint()
      observe(TaskResult(changes = changes))
      previousOnAtomicComplete()
    }
  }

  private fun observe(result: TaskResult) {
    result.changes.forEach { event ->
      val change = event.change
      val gaining = change.gaining
      val removing = change.removing
      when {
        gaining.isProjectCardAt(HAND) &&
            (removing?.className != PROJECT_CARD || removing.hasArgument(SELECTING)) ->
            draw(event.playerOwner(checkNotNull(gaining)), change.count)
        removing.isProjectCardAt(HAND) && gaining?.className == MEGACREDIT ->
            discard(event.playerOwner(checkNotNull(removing)), change.count)
        removing.isProjectCardAt(HAND) &&
            gaining?.className != null &&
            gaining.className != PROJECT_CARD ->
            play(event.playerOwner(checkNotNull(removing)), checkNotNull(gaining).className)
        removing.isProjectCardAt(HAND) && gaining == null ->
            discard(event.playerOwner(checkNotNull(removing)), change.count)
      }
    }
  }

  private fun draw(player: Player, count: Int) {
    repeat(count) {
      expectedDraws[player]?.removeFirstOrNull()
          ?: error("$player gained a ProjectCard without a queued name")
    }
  }

  private fun discard(player: Player, count: Int) {
    repeat(count) {
      val cardClass =
          expectedDiscards[player]?.removeFirstOrNull()
              ?: error("$player discarded a ProjectCard without a queued name")
      check(hand(player).remove(cardClass)) { "$player does not have $cardClass in hand" }
    }
  }

  private fun play(player: Player, cardClass: ClassName) {
    check(hand(player).remove(cardClass)) { "$player played $cardClass without having it in hand" }
  }

  private fun TfmGameplay.expect(
      cardClasses: Array<out ClassName>,
      queueByPlayer: MutableMap<Player, MutableList<ClassName>>,
  ) {
    val player = player
    queueByPlayer.getOrPut(player, ::mutableListOf).addAll(cardClasses)
  }

  private fun hand(player: Player): MutableSet<ClassName> =
      checkNotNull(hands[player]) { "No tracked hand for $player" }

  private fun ChangeEvent.playerOwner(expression: dev.martianzoo.pets.ast.Expression): Player =
      checkNotNull(expression.toComponent(game.reader).playerOwner) {
        "$expression changed without a Player owner in $this"
      }

  private val TfmGameplay.player: Player
    get() = actor as Player

  private companion object {
    val MEGACREDIT: ClassName = cn("Megacredit")
    val PROJECT_CARD: ClassName = cn("ProjectCard")
    val HAND: ClassName = cn("Hand")
    val SELECTING: ClassName = cn("Selecting")
  }

  private fun dev.martianzoo.pets.ast.Expression?.isProjectCardAt(area: ClassName): Boolean =
      this?.className == PROJECT_CARD && hasArgument(area)

  private fun dev.martianzoo.pets.ast.Expression.hasArgument(className: ClassName): Boolean =
      arguments.any {
        it.className == className
      }
}
