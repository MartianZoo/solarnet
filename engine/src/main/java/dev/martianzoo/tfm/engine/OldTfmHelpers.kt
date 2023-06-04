package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Gameplay.OldOperationBody
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp

/** Extension functions to [PlayerSession] for Terraforming Mars-specific APIs. */
object OldTfmHelpers {

  fun PlayerSession.turn(vararg tasks: String, body: OldOperationBody.() -> Unit = {}) {
    if (this.tasks.isEmpty()) {
      operation("NewTurn") {
        tasks.forEach(::task)
        OldOperationBodyImpl().body()
      }
    } else {
      finishOperation(*tasks, body = body)
    }
  }

  fun PlayerSession.playCorp(
      corpName: String,
      buyCards: Int,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit? = {}
  ) {
    require(has("CorporationPhase"))
    return turn(corpName) {
      task(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      tasks.forEach(::task)
      OldOperationBodyImpl().body()
    }
  }

  fun PlayerSession.pass() = turn("Pass")

  fun PlayerSession.stdAction(
      stdAction: String,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit = {}
  ) {
    require(has("ActionPhase"))
    return turn("UseAction1<$stdAction>", *tasks) { body() }
  }

  fun PlayerSession.stdProject(
      stdProject: String,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit = {}
  ) {
    require(has("ActionPhase"))
    return stdAction("UseStandardProject") {
      task("UseAction1<$stdProject>")
      tasks.forEach(::task)
      OldOperationBodyImpl().body()
    }
  }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit = {}
  ) = playCard(cardName, megacredits, steel = 0, *tasks) { body() }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit = {}
  ) = playCard(cardName, megacredits, steel, titanium = 0, *tasks) { body() }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      vararg taskStrings: String,
      body: OldOperationBody.() -> Unit = {}
  ) {

    require(has("ActionPhase"))
    return stdAction("PlayCardFromHand") {
      task("PlayCard<Class<ProjectCard>, Class<$cardName>>")

      fun pay(cost: Int, currency: String) {
        if (cost > 0) ifMatchTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      // TODO should prevent overpayment in actual game rules somehow
      pay(titanium, "Titanium")
      pay(steel, "Steel")
      autoExec()

      val owed = count("Owed")

      // MC really should be equal to owed, but if it's less we might be legitimately testing how
      // the engine responds. We know it doesn't respond usefully to an overage so we check that.
      require(megacredits <= owed) { "Overpaying $megacredits MC when only $owed is owed" }
      pay(megacredits, "Megacredit")

      // Take care of other Accepts we didn't need
      tasks
          .matching { it.cause?.context?.className == cn("Accept") }
          .forEach { writer.reviseTask(it, NoOp) } // "executes" automatically
      autoExec()
      taskStrings.forEach(::task)
      OldOperationBodyImpl().body()
    }
  }

  fun PlayerSession.cardAction1(
      cardName: String,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit = {}
  ) = cardAction(1, cardName, tasks.toList(), body)

  fun PlayerSession.cardAction2(
      cardName: String,
      vararg tasks: String,
      body: OldOperationBody.() -> Unit = {}
  ) = cardAction(2, cardName, tasks.toList(), body)

  private fun PlayerSession.cardAction(
      which: Int,
      cardName: String,
      tasks: List<String>,
      body: OldOperationBody.() -> Unit = {}
  ) {
    require(has(cardName))
    require(has("ActionPhase"))
    return stdAction("UseActionFromCard") {
      task("UseAction$which<$cardName>")
      task("ActionUsedMarker<$cardName>") // will become automatic?
      tasks.forEach(::task)
      OldOperationBodyImpl().body()
    }
  }

  fun PlayerSession.sellPatents(count: Int) {
    require(has("ActionPhase"))
    return stdAction("SellPatents", "$count Megacredit FROM ProjectCard")
  }

  fun PlayerSession.phase(phase: String) {
    asPlayer(Player.ENGINE).operation("${phase}Phase FROM Phase")
  }

  fun PlayerSession.production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith { production(it) }

  fun PlayerSession.production(kind: ClassName) =
      count("PROD[$kind]") - if (kind == MEGACREDIT || kind == cn("M")) 5 else 0

  private val MEGACREDIT = cn("Megacredit")

  fun PlayerSession.oxygenPercent(): Int = count("OxygenStep")
  fun PlayerSession.temperatureC(): Int = -30 + count("TemperatureStep") * 2
  fun PlayerSession.venusPercent(): Int = count("VenusStep") * 2
}
