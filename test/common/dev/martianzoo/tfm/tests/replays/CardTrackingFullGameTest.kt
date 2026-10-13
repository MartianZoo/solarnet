package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.engine.TfmGameplay
import kotlin.test.BeforeTest

/** Replay conveniences for the production card-tracked Agent mode. */
internal abstract class CardTrackingFullGameTest : AbstractFullGameTest() {
  protected open val cardArrivalOrder: Map<ClassName, Map<ClassName, List<ClassName>>> = emptyMap()
  protected open val cardDepartureOrder: Map<ClassName, Map<ClassName, List<ClassName>>> =
      emptyMap()
  protected open val selectingArrivalOrder: Map<ClassName, Map<ClassName, List<ClassName>>> =
      emptyMap()
  protected open val selectingDepartureOrder: Map<ClassName, Map<ClassName, List<ClassName>>> =
      emptyMap()
  protected lateinit var cards: CardTracking
    private set

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()
    cards =
        CardTracking.start(
            game,
            requireSelectingNames =
                selectingArrivalOrder.isNotEmpty() || selectingDepartureOrder.isNotEmpty(),
        )
    val players = game.actors.filterIsInstance<Player>().associateBy { it.className }
    cardArrivalOrder.forEach { (back, byPlayer) ->
      byPlayer.forEach { (player, names) ->
        cards.expectArrivals(checkNotNull(players[player]), *names.toTypedArray(), back = back)
      }
    }
    cardDepartureOrder.forEach { (back, byPlayer) ->
      byPlayer.forEach { (player, names) ->
        cards.expectDepartures(checkNotNull(players[player]), *names.toTypedArray(), back = back)
      }
    }
    selectingArrivalOrder.forEach { (back, byPlayer) ->
      byPlayer.forEach { (player, names) ->
        cards.expectArrivals(
            checkNotNull(players[player]),
            *names.toTypedArray(),
            back = back,
            location = cn("Selecting"),
        )
      }
    }
    selectingDepartureOrder.forEach { (back, byPlayer) ->
      byPlayer.forEach { (player, names) ->
        cards.expectDepartures(
            checkNotNull(players[player]),
            *names.toTypedArray(),
            back = back,
            location = cn("Selecting"),
        )
      }
    }
  }

  /** Names an upcoming hand gain before the operation that draws those cards. */
  protected fun TfmGameplay.draw(vararg cardClasses: ClassName) {
    cards.expectArrivals(actor as Player, *cardClasses)
  }

  /** Names an upcoming anonymous hand removal before the operation that discards those cards. */
  protected fun TfmGameplay.discard(vararg cardClasses: ClassName) {
    cards.expectDepartures(actor as Player, *cardClasses)
  }

  protected fun TfmGameplay.buyCards(vararg cardClasses: ClassName): TaskResult {
    draw(*cardClasses)
    return buyCards(cardClasses.size)
  }

  protected fun TfmGameplay.sellPatents(vararg cardClasses: ClassName): TaskResult {
    discard(*cardClasses)
    return sellPatents(cardClasses.size)
  }

  protected val TfmGameplay.cardsHand: Set<ClassName>
    get() = cards.hand(actor as Player)

  protected fun assertCardTrackingComplete() {
    cards.requireComplete()
  }
}
