package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.ast.ClassName

object Humanizing {

  fun PlayerSession.turn(vararg tasks: String) {
    execute("Turn")
    tasks.forEach(::doTask)
  }

  fun PlayerSession.playCard(cost: Int, cardName: String, vararg tasks: String) {
    turn("UseAction1<PlayCardFromHand>", "PlayCard<Class<$cardName>>")
    if (cost > 0) {
      doTask("$cost Pay<Class<M>> FROM M")
    } else {
      doTask("Ok")
    }
    tasks.forEach(::doTask)
  }

  fun PlayerSession.useCardAction(which: Int, cardName: String, vararg tasks: String) =
      turn(
          "UseAction$which<UseActionFromCard>",
          "UseAction$which<$cardName> THEN ActionUsedMarker<$cardName>",
          *tasks,
      )

  fun PlayerSession.isCardUsed(cardName: String) =
      agent.reader.evaluate(parseAsIs("ActionUsedMarker<$cardName>"))

  fun PlayerSession.stdProject(spName: String, vararg tasks: String) =
      turn("UseAction1<UseStandardProject>", "UseAction1<$spName>", *tasks)

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
