package dev.martianzoo.parity

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Minimal Node-facing session boundary for the dual-engine parity integration. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public class SolarnetSession(
    options: String,
    playerCount: Int,
    resourceReader: (String) -> String,
) {
  private val game: World
  private val workflow: TfmWorkflow.Auto
  private val engine: TfmGameplay
  private val players: List<Player>

  init {
    installResourceReader(resourceReader)
    val playerNames = (1..playerCount).map { "Player$it" }.toTypedArray()
    val premise = Canon.gamePremise(GameConfig(options, *playerNames))
    game = Engine.newGame(premise)
    engine = game.tfm(ENGINE)
    workflow = TfmWorkflow.Auto(game).launch()
    players = game.actors.filterIsInstance<Player>()
  }

  /** Applies one semantic move and returns the resulting snapshot. */
  public fun apply(moveJson: String): String {
    val move = Json.parseToJsonElement(moveJson).jsonObject
    when (move.getValue("operation").jsonPrimitive.content) {
      "selectCorporation" -> {
        val corporation = cn(move.getValue("corporation").jsonPrimitive.content)
        val projectCards = move.getValue("projectCards").jsonPrimitive.int
        game.tfm(movePlayer(move)).playCorp(corporation, projectCards)
      }
      "playProject" -> {
        val card = cn("Card${move.getValue("cardId").jsonPrimitive.content}")
        val payment = move.getValue("payment").jsonObject
        game
            .tfm(movePlayer(move))
            .playProject(
                card,
                megacredits = paymentAmount(payment, "megacredits"),
                steel = paymentAmount(payment, "steel"),
                titanium = paymentAmount(payment, "titanium"),
            )
      }
      else -> error("Unknown parity operation: ${move.getValue("operation")}")
    }
    return snapshot()
  }

  /** Returns the first deliberately small parity snapshot as JSON. */
  public fun snapshot(): String {
    val phase = game.reader.getComponents("Phase").singleOrNull()?.className?.toString()
    return buildJsonObject {
      put("generation", engine.count("Generation"))
      put("phase", phase.orEmpty())
      put("pendingTasks", game.tasks.ids().size)
      put(
          "players",
          buildJsonArray { players.forEach { add(it.className.toString()) } },
      )
    }
        .toString()
  }

  /**
   * Returns every Pets-rendered event at or after [cursor], plus the cursor for the next poll. This
   * complete developer diagnostic is not safe to expose to players.
   */
  public fun eventsSince(cursor: Int): String {
    val nextCursor = game.timeline.checkpoint().ordinal
    val lines = game.events.entriesSince(Checkpoint(cursor))
    return buildJsonObject {
      put("nextCursor", nextCursor)
      put(
          "lines",
          buildJsonArray { lines.forEach { add(game.vocabulary.renderPets(it)) } },
      )
    }
        .toString()
  }

  /** Releases the workflow callback and any suspended workflow work. */
  public fun close() {
    workflow.shutdown()
  }

  private fun installResourceReader(resourceReader: (String) -> String) {
    val global = js("globalThis")
    global.solarnetResourceReader = resourceReader
  }

  private fun movePlayer(move: JsonObject): Player {
    val seat = move.getValue("player").jsonPrimitive.int
    require(seat in 1..players.size) { "Unknown player seat: $seat" }
    return players[seat - 1]
  }

  private fun paymentAmount(payment: JsonObject, resource: String): Int {
    val amount = payment.getValue(resource).jsonPrimitive.int
    require(amount >= 0) { "Negative $resource payment: $amount" }
    return amount
  }
}
