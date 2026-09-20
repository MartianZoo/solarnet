package dev.martianzoo.parity

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.tfm.canon.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.isActionPhaseSecondAction
import dev.martianzoo.tfm.engine.isVisibleInLog
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
    @Suppress("UNUSED_PARAMETER")
    resourceReader: (String) -> String,
) {
  private val agents: Agents
  private val game: World
  private val workflow: TfmWorkflow.Automatic
  private val engine: TfmGameplay
  private val players: List<Player>
  private val corporationSelections = mutableMapOf<Player, Pair<ClassName, Int>>()

  init {
    val playerNames = (1..playerCount).map { "Player$it" }.toTypedArray()
    val premise = Canon.gamePremise(GameConfig(options, *playerNames))
    game = Engine.newGame(premise)
    agents = Agents(game)
    engine = agents.tfm(ADMIN)
    workflow = TfmWorkflow.Automatic(agents).launch()
    players = game.actors.filterIsInstance<Player>()
  }

  /** Applies one semantic move and returns the resulting snapshot. */
  public fun apply(moveJson: String): String {
    val move = Json.parseToJsonElement(moveJson).jsonObject
    when (move.getValue("operation").jsonPrimitive.content) {
      "selectCorporation" -> {
        val player = movePlayer(move)
        val corporation = cn(move.getValue("corporation").jsonPrimitive.content)
        val projectCards = move.getValue("projectCards").jsonPrimitive.int
        require(projectCards in 0..10) {
          "Initial project-card purchase count must be between 0 and 10: $projectCards"
        }
        require(corporationSelections.put(player, corporation to projectCards) == null) {
          "$player already selected a corporation"
        }
        val discarded = 10 - projectCards
        agents[player].doTask(if (discarded == 0) "Ok" else "-$discarded ProjectCard<Selecting>")
        if (internalPhase() == "corporation") {
          corporationSelections.forEach { (selectedPlayer, selection) ->
            val (selectedCorporation, retainedProjects) = selection
            if (!agents.tfm(selectedPlayer).has("CardFront<Class<CorporationCard>>")) {
              agents.tfm(selectedPlayer).playCorp(selectedCorporation, retainedProjects)
            }
          }
        }
      }
      "playProject" -> {
        val card = cardClassForPrintedId(move.getValue("cardId").jsonPrimitive.content)
        val payment = move.getValue("payment").jsonObject
        agents
            .tfm(movePlayer(move))
            .playProject(
                card,
                mc = paymentAmount(payment, "megacredits"),
                steel = paymentAmount(payment, "steel"),
                titanium = paymentAmount(payment, "titanium"),
            )
      }
      "cardAction" -> {
        val card = cardClassForPrintedId(move.getValue("cardId").jsonPrimitive.content)
        agents.tfm(movePlayer(move)).cardAction1(card)
      }
      "buyCards" -> buyCards(move)
      "standardProject" -> startStandardProject(move)
      "convertHeat" -> convertHeat(move)
      "convertPlants" -> convertPlants(move)
      "sellPatents" -> sellPatents(move)
      "placeTile" -> placeTile(move)
      "declineFinalGreenery" -> declineFinalGreenery(move)
      "endTurn" -> agents.tfm(movePlayer(move)).declineSecondAction()
      "pass" -> agents.tfm(movePlayer(move)).pass()
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
      put("gameEnd", phase == "end")
      put("firstPlayer", firstPlayerSeat())
      put("passedPlayers", seatsJson(passedPlayerSeats(phase)))
      put("waitingPlayers", seatsJson(waitingPlayerSeats()))
      put("secondActionPlayers", seatsJson(secondActionPlayerSeats()))
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
            .filter { it.isVisibleInLog(game.reader) }
    return buildJsonObject {
      put("nextCursor", nextCursor)
      put(
          "lines",
          buildJsonArray { lines.forEach { add(it.toString()) } },
      )
    }
        .toString()
  }

  /** Releases the workflow callback and any suspended workflow work. */
  public fun close() {
    workflow.shutdown()
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
      internalPhase().let { if (it == "setup") "corporation" else it }

  private fun internalPhase(): String =
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
        players.filter { agents.tfm(it).has("Pass") }.map(::playerSeat)
      }

  private fun waitingPlayerSeats(): List<Int> =
      game.tasks
          .extract { it.assignee }
          .filterIsInstance<Player>()
          .distinct()
          .map(::playerSeat)
          .sorted()

  private fun secondActionPlayerSeats(): List<Int> =
      game.tasks
          .extract { it }
          .filter { isActionPhaseSecondAction(game, it) }
          .map { it.assignee }
          .filterIsInstance<Player>()
          .map(::playerSeat)
          .distinct()
          .sorted()

  private fun playerSeat(player: Player): Int =
      players.indexOf(player).takeIf { it >= 0 }?.plus(1) ?: error("Unknown seated player: $player")

  private fun seatsJson(seats: List<Int>) = buildJsonArray { seats.forEach(::add) }

  private fun playerSnapshot(player: Player, seat: Int): JsonObject {
    val gameplay = agents.tfm(player)
    return buildJsonObject {
      put("seat", seat)
      put("terraformRating", gameplay.count("TerraformRating"))
      put("victoryPoints", gameplay.count("VictoryPoint"))
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
    game.reader.tfmCatalog.card(cardName)
    return APP_CARD_ID_BY_CLASS.getValue(cardName)
  }

  private fun cardClassForPrintedId(printedId: String): ClassName =
      APP_CARD_CLASS_BY_ID[printedId]
          ?.also(game.reader.tfmCatalog::card)
          ?: error("Unsupported app card ID: $printedId")

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
            val kind = tileKind(tile)
            put("kind", kind)
            if (kind == "ocean") {
              put("owner", JsonNull)
            } else {
              put("owner", playerSeat(getPlayerOwner(game.reader, tile)))
            }
          }
      )
    }
  }

  private fun tileKind(tile: Type): String {
    return when {
      isTileKind(tile, "OceanTile") -> "ocean"
      isTileKind(tile, "GreeneryTile") -> "greenery"
      isTileKind(tile, "CityTile") -> "city"
      else -> error("Unsupported parity tile: $tile")
    }
  }

  private fun isTileKind(tile: Type, kind: String): Boolean =
      tile.rootClass.isSubtypeOf(game.reader.resolve(cn(kind).expression).rootClass)

  private fun startStandardProject(move: JsonObject) {
    val project =
        when (val semanticName = move.getValue("project").jsonPrimitive.content) {
          "powerPlant" -> "PowerPlantProject"
          "asteroid" -> "AsteroidProject"
          "aquifer" -> "AquiferProject"
          "greenery" -> "GreeneryProject"
          "city" -> "CityProject"
          else -> error("Unknown standard project: $semanticName")
        }
    val player = movePlayer(move)
    val gameplay = agents.tfm(player)
    agents[player].continueOperation { gameplay.run { useStdProject(project) } }
  }

  private fun buyCards(move: JsonObject) {
    require(currentPhase() == "research") { "Cards can be bought here only during Research" }
    val count = move.getValue("count").jsonPrimitive.int
    require(count in 0..4) { "Research purchase count must be between 0 and 4: $count" }
    agents.tfm(movePlayer(move)).buyCards(count)
  }

  private fun convertHeat(move: JsonObject) {
    val player = movePlayer(move)
    val gameplay = agents.tfm(player)
    agents[player].continueOperation { gameplay.run { useStdAction("ConvertHeatAction") } }
  }

  private fun convertPlants(move: JsonObject) {
    val player = movePlayer(move)
    val gameplay = agents.tfm(player)
    agents[player].continueOperation { gameplay.run { useStdAction("ConvertPlantsAction") } }
  }

  private fun sellPatents(move: JsonObject) {
    val count = move.getValue("count").jsonPrimitive.int
    require(count > 0) { "Patent sale count must be positive: $count" }
    agents.tfm(movePlayer(move)).sellPatents(count)
  }

  private fun placeTile(move: JsonObject) {
    val area = moveArea(move)
    when (val tile = move.getValue("tile").jsonPrimitive.content) {
      "ocean" -> moveOperation(move).completeOperation { doTask("OceanTile<$area>") }
      "greenery" -> moveOperation(move).completeOperation { doTask("GreeneryTile<$area>") }
      "city" -> moveOperation(move).completeOperation { doTask("CityTile<$area>") }
      else -> error("Unknown tile kind: $tile")
    }
  }

  private fun declineFinalGreenery(move: JsonObject) {
    require(currentPhase() == "finalGreenery") {
      "Final greenery can be declined only during Final Greenery"
    }
    agents.tfm(movePlayer(move)).doTask("Ok")
  }

  private fun moveOperation(move: JsonObject): Agent = agents[movePlayer(move)]

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
    private val APP_CARD_CLASS_BY_ID: Map<String, ClassName> =
        mapOf(
            "B01" to cn("CrediCor"),
            "B04" to cn("InterplanetaryCinematics"),
            "B12" to cn("Teractor"),
            "013" to cn("SpaceElevator"),
            "105" to cn("EarthOffice"),
            "110" to cn("BusinessNetwork"),
            "112" to cn("BribedCommittee"),
        )
    private val APP_CARD_ID_BY_CLASS: Map<ClassName, String> =
        APP_CARD_CLASS_BY_ID.entries.associate { (id, className) -> className to id }
    private val RESOURCE_KINDS: List<Pair<String, ClassName>> =
        listOf(
            "megacredits" to cn("MC"),
            "steel" to cn("Steel"),
            "titanium" to cn("Titanium"),
            "plants" to cn("Plant"),
            "energy" to cn("Energy"),
            "heat" to cn("Heat"),
        )
  }
}
