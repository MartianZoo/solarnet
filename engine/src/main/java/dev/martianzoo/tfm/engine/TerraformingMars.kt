package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.engine.PlayerSession.OperationBody
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp

/**
 * Extension functions to [PlayerSession] for Terraforming Mars-specific APIs.
 */
object TerraformingMars {
  fun PlayerSession.playCorp(
      corpName: String,
      buyCards: Int? = 0,
      body: OperationBody.() -> Unit? = {}
  ) {
    require(has("CorporationPhase"))
    return turn(corpName) {
      task(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      OperationBody().body()
    }
  }

  fun PlayerSession.pass() = turn("Pass").also { require(has("ActionPhase")) }

  fun PlayerSession.stdAction(stdAction: String, body: OperationBody.() -> Unit = {}) {
    require(has("ActionPhase"))
    return turn("UseAction1<$stdAction>", body)
  }

  fun PlayerSession.stdProject(stdProject: String, body: OperationBody.() -> Unit = {}) {
    require(has("ActionPhase"))
    return stdAction("UseStandardProject") {
      task("UseAction1<$stdProject>")
      OperationBody().body()
    }
  }

  fun PlayerSession.playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: OperationBody.() -> Unit = {}
  ) {

    require(has("ActionPhase"))
    return stdAction("PlayCardFromHand") {
      task("PlayCard<Class<$cardName>>")

      fun pay(cost: Int, currency: String) {
        if (cost > 0) ifMatchTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      pay(megacredits, "Megacredit")
      pay(steel, "Steel")
      pay(titanium, "Titanium")

      // Take care of other Accepts we didn't need
      for (task in tasks.toList()) {
        if (task.cause?.context?.className == ClassName.cn("Accept")) {
          writer.narrowTask(task.id, NoOp) // "executes" automatically
        }
      }
      autoExec()
      OperationBody().body()
    }
  }

  fun PlayerSession.cardAction(
      cardName: String,
      which: Int = 1,
      body: OperationBody.() -> Unit = {}
  ) {
    require(has(cardName))
    require(has("ActionPhase"))
    return stdAction("UseActionFromCard") {
      task("UseAction$which<$cardName>")
      task("ActionUsedMarker<$cardName>") // TODO slight problem for Viron?
      OperationBody().body()
    }
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
