package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.engine.PlayerSession.Tasker
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp

/**
 * Extension functions that translate between the raw engine language and the way humans tend to
 * think about the game.
 */
object Humanize {
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
      task(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      theTasker.tasker()
    }
  }

  fun PlayerSession.pass() = turn("Pass").also { require(has("ActionPhase")) }

  public fun PlayerSession.stdAction(stdAction: String) = stdAction(stdAction) {}

  fun <T : Any> PlayerSession.stdAction(stdAction: String, tasker: Tasker.() -> T?): T? {
    require(has("ActionPhase"))
    return turn("UseAction1<$stdAction>", tasker)
  }

  fun PlayerSession.stdProject(stdProject: String) = stdProject(stdProject) {}

  fun <T : Any> PlayerSession.stdProject(stdProject: String, tasker: Tasker.() -> T?): T? {
    return stdAction("UseStandardProject") {
      task("UseAction1<$stdProject>")
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
      task("PlayCard<Class<$cardName>>")

      fun pay(cost: Int, currency: String) {
        if (cost > 0) session.ifMatchTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      pay(megacredits, "Megacredit")
      pay(steel, "Steel")
      pay(titanium, "Titanium")

      // Take care of other Accepts we didn't need
      for (task in tasks().toList()) {
        if (task.cause?.context?.className == ClassName.cn("Accept")) {
          session.writer.narrowTask(task.id, NoOp) // "executes" automatically
        }
      }
      autoExec()
      val result = theTasker.tasker()
      autoExec()
      result
    }
  }

  fun PlayerSession.cardAction(cardName: String, actionNumber: Int = 1) {
    cardAction(cardName, actionNumber) {}
  }

  fun <T : Any> PlayerSession.cardAction(cardName: String, which: Int = 1, tasker: Tasker.() -> T?): T? {
    require(has(cardName))
    return stdAction("UseActionFromCard") {
      task("UseAction$which<$cardName>")
      task("ActionUsedMarker<$cardName>") // TODO slight problem for Viron?
      theTasker.tasker()
    }
  }

  // OLD STUFF - TODO GET RID OF

  fun PlayerSession.counts(s: String) = s.split(",").map(::count)

  fun PlayerSession.production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith {
        count("PROD[$it]") - if (it == MEGACREDIT) 5 else 0
      }

  private val MEGACREDIT = ClassName.cn("Megacredit")

  fun PlayerSession.oxygenPercent(): Int = count("OxygenStep")
  fun PlayerSession.temperatureC(): Int = -30 + count("TemperatureStep") * 2
  fun PlayerSession.venusPercent(): Int = count("VenusStep") * 2
}
