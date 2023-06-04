package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

public class TerraformingMarsApi(
    val game: Gameplay.TurnLayer,
    val tasks: TaskQueue,
    val reader: GameReader, // TODO ditch it
    val player: Player
) {
  constructor(
      game: Game,
      player: Player
  ) : this(game.gameplay(player).turnLayer(), game.tasks, game.reader, player)

  fun sneak(instruction: String): TaskResult = (game as Gameplay.ChangeLayer).sneak(instruction)

  fun playCorp(corpName: String, buyCards: Int, body: BodyLambda = {}): TaskResult {
    return game.turn {
      doTask(corpName) // TODO playcard
      doTask(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      body()
    }
  }

  fun pass(): TaskResult = game.turn { doTask("Pass") }

  fun stdAction(stdAction: String, body: BodyLambda = {}): TaskResult {
    return game.turn {
      doTask("UseAction1<$stdAction>")
      body()
    }
  }

  fun stdProject(stdProject: String, body: BodyLambda = {}): TaskResult {
    return stdAction("UseStandardProject") {
      doTask("UseAction1<$stdProject>")
      body()
    }
  }

  fun playCard(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: BodyLambda = {}
  ): TaskResult {
    return game.turn {
      if (tasks.matching { "${it.instruction}".contains("StandardAction") }.any()) {
        doTask("UseAction1<PlayCardFromHand>")
      }
      doTask("PlayCard<Class<ProjectCard>, Class<$cardName>>")

      fun pay(cost: Int, currency: String) {
        if (cost > 0) doTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      // TODO should prevent overpayment in actual game rules somehow
      pay(titanium, "Titanium")
      pay(steel, "Steel")

      val owed = game.count("Owed")

      // MC really should be equal to owed, but if it's less we might be legitimately testing how
      // the engine responds. We know it doesn't respond usefully to an overage so we check that.
      require(megacredits <= owed) { "Overpaying $megacredits MC when only $owed is owed" }
      pay(megacredits, "Megacredit")

      // Take care of other Accepts we didn't need
      tasks
          .matching { it.cause?.context?.className == cn("Accept") }
          .forEach { game.reviseTask(it, "Ok") } // "executes" automatically
      autoExecNow()
      body()
      autoExecNow()
    }
  }

  fun cardAction1(cardName: String, body: BodyLambda = {}) = cardAction(1, cardName, body)
  fun cardAction2(cardName: String, body: BodyLambda = {}) = cardAction(2, cardName, body)

  private fun cardAction(which: Int, cardName: String, body: BodyLambda = {}): TaskResult {
    return stdAction("UseActionFromCard") {
      doTask("UseAction$which<$cardName>")
      doTask("ActionUsedMarker<$cardName>") // will become automatic?
      body()
    }
  }

  fun sellPatents(count: Int) =
      stdAction("SellPatents") { doTask("$count Megacredit FROM ProjectCard") }

  fun phase(phase: String) {
    require(player == ENGINE)
    game.operationLayer().initiate("${phase}Phase FROM Phase")
  }

  fun production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith { production(it) }

  fun production(kind: ClassName) =
      game.count("PROD[$kind]") - if (kind == MEGACREDIT || kind == cn("M")) 5 else 0

  private val MEGACREDIT = cn("Megacredit")

  fun oxygenPercent(): Int = game.count("OxygenStep")
  fun temperatureC(): Int = -30 + game.count("TemperatureStep") * 2
  fun venusPercent(): Int = game.count("VenusStep") * 2
}
