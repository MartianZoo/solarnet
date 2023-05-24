package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.engine.PlayerSession.OperationBody
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp

/** Extension functions to [PlayerSession] for Terraforming Mars-specific APIs. */
object TerraformingMars {
  fun PlayerSession.playCorp(
      corpName: String,
      buyCards: Int? = 0,
      vararg tasks: String,
      body: OperationBody.() -> Unit? = {}
  ) {
    require(has("CorporationPhase"))
    return turn(corpName) {
      task(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      tasks.forEach(::task)
      OperationBody().body()
    }
  }

  fun PlayerSession.pass() = turn("Pass")

  fun PlayerSession.stdAction(
      stdAction: String,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) {
    require(has("ActionPhase"))
    return turn("UseAction1<$stdAction>", *tasks) { body() }
  }

  fun PlayerSession.stdProject(
      stdProject: String,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) {
    require(has("ActionPhase"))
    return stdAction("UseStandardProject") {
      task("UseAction1<$stdProject>")
      tasks.forEach(::task)
      OperationBody().body()
    }
  }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) = playCard(cardName, megacredits, steel = 0, *tasks) { body() }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) = playCard(cardName, megacredits, steel, titanium = 0, *tasks) { body() }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      vararg taskStrings: String,
      body: OperationBody.() -> Unit = {}
  ) {

    require(has("ActionPhase"))
    return stdAction("PlayCardFromHand") {
      task("PlayCard<Class<ProjectCard>, Class<$cardName>>")

      fun pay(cost: Int, currency: String) {
        if (cost > 0) ifMatchTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      pay(megacredits, "Megacredit")
      pay(steel, "Steel")
      pay(titanium, "Titanium")

      // Take care of other Accepts we didn't need
      tasks.matching { it.cause?.context?.className == cn("Accept") }
          .forEach { writer.narrowTask(it, NoOp) } // "executes" automatically
      autoExec()
      taskStrings.forEach(::task)
      OperationBody().body()
    }
  }

  fun PlayerSession.cardAction1(
      cardName: String,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) = cardAction(1, cardName, tasks.toList(), body)

  fun PlayerSession.cardAction2(
      cardName: String,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) = cardAction(2, cardName, tasks.toList(), body)

  fun PlayerSession.cardAction3(
      cardName: String,
      vararg tasks: String,
      body: OperationBody.() -> Unit = {}
  ) = cardAction(3, cardName, tasks.toList(), body)

  private fun PlayerSession.cardAction(
      which: Int,
      cardName: String,
      tasks: List<String>,
      body: OperationBody.() -> Unit = {}
  ) {
    require(has(cardName))
    require(has("ActionPhase"))
    return stdAction("UseActionFromCard") {
      task("UseAction$which<$cardName>")
      task("ActionUsedMarker<$cardName>") // will become automatic?
      tasks.forEach(::task)
      OperationBody().body()
    }
  }

  fun PlayerSession.sellPatents(count: Int) {
    require(has("ActionPhase"))
    return stdAction("SellPatents", "$count Megacredit FROM ProjectCard")
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
