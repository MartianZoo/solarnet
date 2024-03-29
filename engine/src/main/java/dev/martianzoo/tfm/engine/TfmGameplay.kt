package dev.martianzoo.tfm.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.USE_ACTION
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT

/**
 * Wraps and extends a [Gameplay] instance to provide much more convenient functions specific to
 * *Terraforming Mars*.
 */
public class TfmGameplay(
    private val game: Game,
    override val player: Player,
    internal val gameplay: TurnLayer = game.gameplay(player) as TurnLayer,
) : TurnLayer by gameplay {

  val reader: GameReader by game::reader

  fun asPlayer(player: Player) = TfmGameplay(game, player)

  fun nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(Player.players(5))) {
        asPlayer(player).doTask(if (cards > 0) "$cards BuyCard" else "Ok")
      }
    }
    phase("Action")
  }

  fun playCorp(cardName: String, buyCards: Int, body: BodyLambda = {}): TaskResult {
    return turn {
      doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
      doTask(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      body()
    }
  }

  fun pass(): TaskResult = turn { doTask("Pass") }

  fun stdAction(stdAction: String, which: Int = 1, body: BodyLambda = {}): TaskResult {
    return turn {
      doTask("UseAction$which<$stdAction>")
      body()
    }
  }

  fun stdProject(stdProject: String, body: BodyLambda = {}): TaskResult {
    return stdAction("UseStandardProjectSA") {
      doTask("UseAction1<$stdProject>")
      body()
    }
  }

  fun playPrelude(cardName: String, body: BodyLambda = {}): TaskResult {
    return turn {
      doTask("PlayCard<Class<PreludeCard>, Class<$cardName>>")
      body()
    }
  }

  fun playProject(cardName: String, body: BodyLambda = {}): Nothing =
      error("you must specify some cost")

  fun playProject(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: BodyLambda = {}
  ): TaskResult {
    return turn {
      if (tasks.matching { "${it.instruction}".contains("StandardAction") }.any()) {
        doFirstTask("UseAction1<PlayCardSA>") // "first" because HeadStart
      }
      doTask("PlayCard<Class<ProjectCard>, Class<$cardName>>")

      pay(megacredits, steel, titanium)
      body()
      autoExecNow()
    }
  }
  fun pay(
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
  ): TaskResult {
    return godMode().continueManual {
      fun pay(cost: Int, currency: String) {
        if (cost > 0) doTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      // Should prevent overpayment in actual game rules somehow (#19)
      pay(titanium, "Titanium")
      pay(steel, "Steel")

      val owed = count("Owed")

      // MC really should be equal to owed, but if it's less we might be legitimately testing how
      // the engine responds. We know it doesn't respond usefully to an overage so we check that.
      require(megacredits <= owed) { "Overpaying $megacredits MC when only $owed is owed" }
      pay(megacredits, "Megacredit")

      // Take care of other Accepts we didn't need
      tasks
          .matching { it.cause?.context?.className == cn("Accept") }
          .forEach { reviseTask(it, "Ok") } // "executes" automatically
      autoExecNow()
    }
  }

  fun cardAction1(cardName: String, body: BodyLambda = {}) = cardAction(1, cardName, body)
  fun cardAction2(cardName: String, body: BodyLambda = {}) = cardAction(2, cardName, body)

  private fun cardAction(which: Int, cardName: String, body: BodyLambda = {}): TaskResult {
    return stdAction("UseCardActionSA") {
      doTask("$USE_ACTION$which<$cardName>")
      doTask("ActionUsedMarker<$cardName>") // will become automatic?
      body()
    }
  }

  fun sellPatents(count: Int) =
      stdAction("SellPatents") { doTask("-$count ProjectCard THEN $count") }

  fun phase(phase: String, body: BodyLambda = {}) {
    asPlayer(ENGINE).godMode().manual("${phase}Phase FROM Phase", body)
  }

  fun production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith { production(it) }

  fun production(kind: ClassName) =
      count("PROD[$kind]") - if (kind == MEGACREDIT || kind == cn("M")) 5 else 0

  fun oxygenPercent(): Int = count("OxygenStep")
  fun temperatureC(): Int = -30 + count("TemperatureStep") * 2
  fun venusPercent(): Int = count("VenusStep") * 2

  companion object {
    fun Game.tfm(player: Player) = TfmGameplay(this, player)
  }
}
