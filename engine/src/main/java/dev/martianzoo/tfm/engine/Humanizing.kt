package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.pets.ast.ClassName

/**
 * Extension functions that translate between the raw engine language and the way humans tend to
 * think about the game.
 */
object Humanizing {

  fun PlayerSession.startTurn(vararg tasks: String) {
    asPlayer(ENGINE).initiate("NewTurn<$player>")
    tasks.forEach {
      tryMatchingTask(it)
      tryToDrain()
    }
  }

  fun PlayerSession.fullTurn(vararg tasks: String) {
    startTurn(*tasks)
    mustDrain()
  }

  fun PlayerSession.startPlayCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0
  ) {
    startTurn("UseAction1<PlayCardFromHand>", "PlayCard<Class<$cardName>>")
    if (megacredits > 0) tryMatchingTask("$megacredits Pay<Class<M>> FROM M")
    if (steel > 0) tryMatchingTask("$steel Pay<Class<S>> FROM S")
    if (titanium > 0) tryMatchingTask("$titanium Pay<Class<T>> FROM T")
    try {
      while (true) doFirstTask("Ok")
    } catch (ignore: Exception) {}
  }

  fun PlayerSession.useCardAction(which: Int, cardName: String, vararg tasks: String) =
      fullTurn(
          "UseAction1<UseActionFromCard>",
          "UseAction$which<$cardName>",
          "ActionUsedMarker<$cardName>",
          *tasks,
      )

  fun PlayerSession.stdProject(spName: String, vararg tasks: String) =
      fullTurn("UseAction1<UseStandardProject>", "UseAction1<$spName>", *tasks)

  fun PlayerSession.counts(s: String) = s.split(",").map(::count)

  fun PlayerSession.production(): Map<ClassName, Int> =
      ApiUtils.standardResourceNames(game.reader).associateWith {
        val type = PRODUCTION.of(player.expression, it.classExpression())
        countComponent(game.toComponent(type)) - if (it == MEGACREDIT) 5 else 0
      }

  private val MEGACREDIT = ClassName.cn("Megacredit")
  private val PRODUCTION = ClassName.cn("Production")

  fun PlayerSession.oxygenPercent(): Int = count("OxygenStep")
  fun PlayerSession.temperatureC(): Int = -30 + count("TemperatureStep") * 2
  fun PlayerSession.venusPercent(): Int = count("VenusStep") * 2
}
