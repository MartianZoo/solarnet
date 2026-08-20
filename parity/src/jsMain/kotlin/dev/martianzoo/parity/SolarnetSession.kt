package dev.martianzoo.parity

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.Player
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.OperationLayer
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.engine.isHiddenFromLog
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.tfm.api.tfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.types.Type
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
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
      "cardAction" -> {
        val card = cn("Card${move.getValue("cardId").jsonPrimitive.content}")
        game.tfm(movePlayer(move)).cardAction1(card)
      }
      "standardProject" -> startStandardProject(move)
      "placeTile" -> placeTile(move)
      "endTurn" -> game.tfm(movePlayer(move)).declineSecondAction()
      "pass" -> game.tfm(movePlayer(move)).doTask("Pass")
      else -> error("Unknown parity operation: ${move.getValue("operation")}")
    }
    return snapshot()
  }

  /** Returns the first normalized public-state parity slice as JSON. */
  public fun snapshot(): String {
    val phase = currentPhase()
    return buildJsonObject {
      put("generation", engine.count("Generation"))
      put("phase", phase)
      put("firstPlayer", firstPlayerSeat())
      put("passedPlayers", seatsJson(passedPlayerSeats(phase)))
      put(
          "players",
          buildJsonArray {
            players.forEachIndexed { index, player -> add(playerSnapshot(player, index + 1)) }
          },
      )
      put(
          "globalParameters",
          buildJsonObject {
            put("temperature", engine.temperatureC())
            put("oxygen", engine.oxygenPercent())
            put("oceans", engine.count("OceanTile"))
          },
      )
      put("tiles", tilesSnapshot())
    }
        .toString()
  }

  /**
   * Returns the ordinary-log changes at or after [cursor], plus the cursor for the next poll. Task
   * events and non-phase Hidden changes remain available in the World's complete event log.
   */
  public fun eventsSince(cursor: Int): String {
    val nextCursor = game.timeline.checkpoint().ordinal
    val lines =
        game.events
            .entriesSince(Checkpoint(cursor))
            .filterIsInstance<ChangeEvent>()
            .filterNot(game.reader::isHiddenFromLog)
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

  private fun currentPhase(): String =
      game.reader
          .getComponents("Phase")
          .single()
          .className
          .toString()
          .removeSuffix("Phase")
          .replaceFirstChar { it.lowercase() }

  private fun firstPlayerSeat(): Int =
      playerSeat(getPlayerOwner(game.reader, game.reader.getComponents("StartToken").single()))

  private fun passedPlayerSeats(phase: String): List<Int> =
      if (phase != "action") {
        emptyList()
      } else {
        players.filter { game.tfm(it).has("Pass") }.map(::playerSeat)
      }

  private fun playerSeat(player: Player): Int =
      players.indexOf(player).takeIf { it >= 0 }?.plus(1) ?: error("Unknown seated player: $player")

  private fun seatsJson(seats: List<Int>) = buildJsonArray { seats.forEach(::add) }

  private fun playerSnapshot(player: Player, seat: Int): JsonObject {
    val gameplay = game.tfm(player)
    return buildJsonObject {
      put("seat", seat)
      put("terraformRating", gameplay.count("TerraformRating"))
      put("resources", resourceSnapshot(gameplay, production = false))
      put("production", resourceSnapshot(gameplay, production = true))
      put("handCount", gameplay.count("ProjectCard"))
      put(
          "playedCardIds",
          buildJsonArray { playedCardIds(player).forEach(::add) },
      )
    }
  }

  private fun resourceSnapshot(gameplay: TfmGameplay, production: Boolean): JsonObject =
      buildJsonObject {
        RESOURCE_KINDS.forEach { (key, kind) ->
          put(key, if (production) gameplay.production(kind) else gameplay.count(kind.toString()))
        }
      }

  private fun playedCardIds(player: Player): List<String> =
      game.reader
          .let {
            it.getComponents("CardFront").toList() + it.getComponents("PlayedEvent").toList()
          }
          .asSequence()
          .filter { getPlayerOwner(game.reader, it) == player }
          .map(::cardId)
          .sorted()
          .toList()

  private fun cardId(component: Type): String {
    val cardName =
        if (component.className == PLAYED_EVENT) {
          component.expressionFull.arguments
              .filter { it.className == CLASS }
              .map { it.arguments.single().className }
              .single()
        } else {
          component.className
        }
    return game.reader.tfmAuthority.card(cardName).id
  }

  private fun tilesSnapshot() = buildJsonArray {
    val areas = mapDefinition(game.reader).areas.rows().flatten().filterNotNull()
    val areaByName = areas.associateBy { it.className }
    val tiles =
        game.reader
            .getComponents("Tile")
            .asSequence()
            .map { tile ->
              val matchingAreas =
                  tile.typeDependencies.mapNotNull { areaByName[it.boundType.className] }
              require(matchingAreas.size == 1) { "Unsupported parity tile area: $tile" }
              val area = matchingAreas.single()
              area to tile
            }
            .sortedWith(compareBy({ it.first.row }, { it.first.column }))

    tiles.forEach { (area, tile) ->
      add(
          buildJsonObject {
            put("row", area.row)
            put("column", area.column)
            put("kind", tileKind(tile))
            put("owner", JsonNull)
          }
      )
    }
  }

  private fun tileKind(tile: Type): String {
    require(isTileKind(tile, "OceanTile")) { "Unsupported parity tile: $tile" }
    return "ocean"
  }

  private fun isTileKind(tile: Type, kind: String): Boolean =
      tile.rootClass.isSubtypeOf(game.reader.resolve(cn(kind).expression).rootClass)

  private fun startStandardProject(move: JsonObject) {
    when (val project = move.getValue("project").jsonPrimitive.content) {
      "aquifer" ->
          moveOperation(move).continueManual {
            doTask("UseAction1<UseStandardProjectSA>")
            doTask("UseAction1<AquiferSP>")
          }
      else -> error("Unknown standard project: $project")
    }
  }

  private fun placeTile(move: JsonObject) {
    val area = moveArea(move)
    when (val tile = move.getValue("tile").jsonPrimitive.content) {
      "ocean" -> moveOperation(move).finish { doTask("OceanTile<$area>") }
      else -> error("Unknown tile kind: $tile")
    }
  }

  private fun moveOperation(move: JsonObject): OperationLayer =
      game.gameplay(movePlayer(move)) as OperationLayer

  private fun moveArea(move: JsonObject): ClassName {
    val spaceId = move.getValue("spaceId").jsonPrimitive.content
    require(spaceId.length == 2 && spaceId.all { it in '0'..'9' }) {
      "Malformed app space ID: $spaceId"
    }
    val areas = mapDefinition(game.reader).areas.rows().flatten().filterNotNull()
    return areas.getOrNull(spaceId.toInt() - FIRST_APP_SPACE_ID)?.className
        ?: error("Unknown app space ID: $spaceId")
  }

  private companion object {
    const val FIRST_APP_SPACE_ID = 3

    private val PLAYED_EVENT: ClassName = cn("PlayedEvent")
    private val RESOURCE_KINDS: List<Pair<String, ClassName>> =
        listOf(
            "megacredits" to cn("Megacredit"),
            "steel" to cn("Steel"),
            "titanium" to cn("Titanium"),
            "plants" to cn("Plant"),
            "energy" to cn("Energy"),
            "heat" to cn("Heat"),
        )
  }
}
