package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.engine.PlayerSession.Tasker
import dev.martianzoo.tfm.pets.ast.ClassName

/**
 * Extension functions that translate between the raw engine language and the way humans tend to
 * think about the game.
 */
object Humanize {

  fun PlayerSession.turn(initial: String? = null) {
    turn(initial) {}
  }

  fun <T : Any> PlayerSession.turn(initial: String? = null, tasker: Tasker.() -> T?): T? {
    return action("$NEW_TURN") {
      initial?.let(::doFirstTask)
      theTasker.tasker()
    }
  }

  fun PlayerSession.playCorp(corpName: String, buyCards: Int? = 0) {
    playCorp(corpName, buyCards) {}
  }

  fun <T : Any> PlayerSession.playCorp(
      corpName: String,
      buyCards: Int? = 0,
      tasker: Tasker.() -> T?
  ): T? {
    require(has("CorporationPhase"))
    return turn(corpName) {
      val result = theTasker.tasker()
      doFirstTask(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      result
    }
  }

  fun PlayerSession.pass() = turn("Pass").also { require(has("ActionPhase")) }

  fun PlayerSession.stdAction(stdAction: String) {
    stdAction(stdAction) {}
  }

  fun <T : Any> PlayerSession.stdAction(stdAction: String, tasker: Tasker.() -> T?): T? {
    require(has("ActionPhase"))
    return turn("UseAction1<$stdAction>", tasker)
  }

  fun PlayerSession.stdProject(stdProject: String) {
    stdProject(stdProject) {}
  }

  fun <T : Any> PlayerSession.stdProject(stdProject: String, tasker: Tasker.() -> T?): T? {
    return stdAction("UseStandardProject") {
      doFirstTask("UseAction1<$stdProject>")
      theTasker.tasker()
    }
  }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
  ) {
    playCard(cardName, megacredits, steel, titanium) {}
  }

  fun <T : Any> PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      tasker: Tasker.() -> T?
  ): T? {

    return stdAction("PlayCardFromHand") {
      doFirstTask("PlayCard<Class<$cardName>>")

      fun pay(cost: Int, currency: String) {
        if (cost > 0) tryMatchingTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      pay(megacredits, "Megacredit")
      pay(steel, "Steel")
      pay(titanium, "Titanium")

      // Take care of other Accepts we didn't need
      for (task in tasks()) {
        if (task.cause?.context?.className == ClassName.cn("Accept")) {
          tryTask(task.id, "Ok")
        }
      }

      theTasker.tasker()
    }
  }

  fun PlayerSession.cardAction(cardName: String, actionNumber: Int = 1) {
    cardAction(cardName, actionNumber) {}
  }

  fun <T : Any> PlayerSession.cardAction(cardName: String, which: Int = 1, tasker: Tasker.() -> T?): T? {
    require(has(cardName))
    return stdAction("UseActionFromCard") {
      doFirstTask("UseAction$which<$cardName>")
      val result = theTasker.tasker()
      doFirstTask("ActionUsedMarker<$cardName>")
      result
    }
  }

  // OLD STUFF - TODO GET RID OF

  fun PlayerSession.startTurn(vararg tasks: String) {
    atomic {
      initiate("NewTurn")
      tasks.forEach(::tryMatchingTask)
    }
  }

  fun PlayerSession.useCardAction(which: Int, cardName: String, vararg tasks: String) =
      startTurn(
          "UseAction1<UseActionFromCard>",
          "UseAction$which<$cardName>",
          "ActionUsedMarker<$cardName>",
          *tasks,
      )

  fun PlayerSession.stdProject(spName: String, vararg tasks: String) =
      startTurn("UseAction1<UseStandardProject>", "UseAction1<$spName>", *tasks)

  fun PlayerSession.counts(s: String) = s.split(",").map(::count)

  fun PlayerSession.production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith {
        count("PROD[$it]") - if (it == MEGACREDIT) 5 else 0
      }

  private val NEW_TURN = ClassName.cn("NewTurn")
  private val MEGACREDIT = ClassName.cn("Megacredit")

  fun PlayerSession.oxygenPercent(): Int = count("OxygenStep")
  fun PlayerSession.temperatureC(): Int = -30 + count("TemperatureStep") * 2
  fun PlayerSession.venusPercent(): Int = count("VenusStep") * 2
}
