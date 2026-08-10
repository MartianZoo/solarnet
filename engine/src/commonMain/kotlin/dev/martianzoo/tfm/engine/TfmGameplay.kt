package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.USE_ACTION
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.Player
import dev.martianzoo.data.Task
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT

/**
 * Wraps and extends a [Gameplay] instance to provide much more convenient functions specific to
 * *Terraforming Mars*.
 */
public class TfmGameplay(
    private val game: World,
    override val actor: Actor,
    internal val gameplay: TurnLayer = game.gameplay(actor) as TurnLayer,
) : TurnLayer by gameplay {

  public val reader: GameReader by game::reader

  internal fun asActor(actor: Actor) = TfmGameplay(game, actor)

  public fun asPlayer(player: Player): TfmGameplay = asActor(player)

  public fun nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    asActor(ENGINE).godMode().manual("Generation")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(Player.players(5))) {
        asPlayer(player).doTask(if (cards > 0) "$cards BuyCard" else "Ok")
      }
    }
    phase("Action")
  }

  public fun playCorp(cardName: String, buyCards: Int, body: BodyLambda = {}): TaskResult {
    return turn {
      doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
      doTask(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      body()
    }
  }

  public fun pass(): TaskResult = turn { doTask("Pass") }

  public fun declineSecondAction(): TaskResult {
    val actorTasks = game.tasks.extract { it }.filter { it.assignee == actor }
    val secondAction =
        actorTasks
            .withIndex()
            .filter { (_, task) -> task.isActionPhaseSecondAction() }
            .singleOrNull()
            ?: throw TaskException("$actor is not waiting on exactly one second-action offer")
    return doTask("Ok", secondAction.index + 1)
  }

  private fun Task.isActionPhaseSecondAction(): Boolean {
    val origin = cause ?: return false
    if (origin.context.className != cn("ActionPhase")) return false
    val trigger = game.events.entryAt(origin.triggerEvent) as? ChangeEvent
    return trigger?.change?.gaining?.className == cn("SecondAction")
  }

  public fun stdAction(stdAction: String, which: Int = 1, body: BodyLambda = {}): TaskResult {
    return turn {
      doTask("UseAction$which<$stdAction>")
      body()
    }
  }

  public fun stdProject(stdProject: String, body: BodyLambda = {}): TaskResult {
    return stdAction("UseStandardProjectSA") {
      doTask("UseAction1<$stdProject>")
      body()
    }
  }

  public fun playPrelude(cardName: String, body: BodyLambda = {}): TaskResult {
    return turn {
      doTask("PlayCard<Class<PreludeCard>, Class<$cardName>>")
      body()
    }
  }

  // In the method after this, all the cost parameters are optional,
  // but you've gotta provide ONE of them.
  public fun playProject(unused1: String, unused2: BodyLambda = {}): Nothing =
      error("you must specify some cost")

  public fun playProject(
      cardName: String,
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: BodyLambda = {},
  ): TaskResult {
    return turn {
      if (tasks.matching { "${it.instruction}".contains("StandardAction") }.any()) {
        doTask("UseAction1<PlayCardSA>")
      }
      doTask("PlayCard<Class<ProjectCard>, Class<$cardName>>")

      pay(megacredits, steel, titanium)
      body()
      autoExecNow()
    }
  }

  public fun pay(
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
      if (megacredits > owed) {
        throw LimitsException("Overpaying $megacredits MC when only $owed is owed")
      }
      pay(megacredits, "Megacredit")

      // Take care of other Accepts we didn't need
      tasks
          .matching { it.cause?.context?.className == cn("Accept") }
          .forEach { reviseTask(it, "Ok") } // "executes" automatically
      autoExecNow()
    }
  }

  public fun cardAction1(cardName: String, body: BodyLambda = {}): TaskResult =
      cardAction(1, cardName, body)

  public fun cardAction2(cardName: String, body: BodyLambda = {}): TaskResult =
      cardAction(2, cardName, body)

  private fun cardAction(which: Int, cardName: String, body: BodyLambda = {}): TaskResult {
    return stdAction("UseCardActionSA") {
      doTask("ActionUsedMarker<$cardName>")
      doTask("$USE_ACTION$which<$cardName>")
      body()
    }
  }

  public fun sellPatents(count: Int): TaskResult =
      stdAction("SellPatents") { doTask("-$count ProjectCard THEN $count") }

  public fun phase(phase: String, body: BodyLambda = {}) {
    if (count("Phase") != 1) {
      throw NotNowException(
          "No current Phase; start SetupPhase through TfmWorkflow before changing phases"
      )
    }
    asActor(ENGINE).godMode().manual("${phase}Phase FROM Phase", body)
  }

  internal fun production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith { production(it) }

  public fun production(kind: ClassName): Int =
      count("PROD[$kind]") - if (kind == MEGACREDIT || kind == cn("M")) 5 else 0

  public fun oxygenPercent(): Int = count("OxygenStep")

  public fun temperatureC(): Int = -30 + count("TemperatureStep") * 2

  public fun venusPercent(): Int = count("VenusStep") * 2

  public companion object {
    public fun World.tfm(actor: Actor): TfmGameplay = TfmGameplay(this, actor)
  }
}
