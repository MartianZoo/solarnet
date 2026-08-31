package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.engine.ComponentGraph.CountSubscription
import dev.martianzoo.engine.GameRecording
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.visibleLogEvents
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.KeyboardEvent

private const val BENCHMARK_PREFIX = "game-viewer"

public fun main() {
  val gameSelect = document.getElementById("game-select") as HTMLSelectElement
  val status = checkNotNull(document.getElementById("status"))
  val positionLabel = checkNotNull(document.getElementById("position-label"))
  var recording: GameRecording? = null
  var recordingName = ""
  var selectedPlayerIndex = 0
  var selectablePositions = emptyList<Int>()
  var mapSubscriptions = emptyList<CountSubscription>()

  val placeholder = document.createElement("option")
  placeholder.setAttribute("value", "")
  placeholder.setAttribute("disabled", "")
  placeholder.setAttribute("selected", "")
  placeholder.textContent = "Select a game…"
  gameSelect.appendChild(placeholder)

  SavedGames.all.forEachIndexed { index, savedGame ->
    val option = document.createElement("option")
    option.setAttribute("value", index.toString())
    option.textContent = savedGame.name
    gameSelect.appendChild(option)
  }

  fun updatePosition(active: GameRecording, scrollLog: Boolean) {
    val players = active.world.actors.filterIsInstance<Player>()
    selectedPlayerIndex = selectedPlayerIndex.coerceIn(players.indices)
    val player = players[selectedPlayerIndex]
    val checkpoint = active.positions[active.positionIndex]
    val displayedIndex = selectablePositions.indexOf(active.positionIndex)
    positionLabel.textContent =
        "Position ${displayedIndex + 1} of ${selectablePositions.size} · event ${checkpoint.ordinal}"
    status.textContent = recordingName
    measurePhase("render.player-tabs-update") { updatePlayerTabs(active, selectedPlayerIndex) }
    measurePhase("render.dashboard") { renderDashboard(active, player) }
    measurePhase("render.cards") { renderCards(active, player) }
    measurePhase("render.log-state") { updateLogState(active) }
    if (scrollLog) measurePhase("render.log-scroll") { scrollActiveLogStop() }
  }

  fun showPosition(active: GameRecording, index: Int, scrollLog: Boolean = true) {
    if (index !in active.positions.indices) return
    active.seek(index)
    updatePosition(active, scrollLog)
  }

  fun loadSelectedGame() {
    if (gameSelect.value.isEmpty()) return
    val selected = SavedGames.all[gameSelect.value.toInt()]
    gameSelect.disabled = true
    status.textContent = "Replaying ${selected.name}…"
    window.setTimeout(
        {
          try {
            clearBenchmarkEntries()
            mark("load.start")
            mapSubscriptions.forEach(CountSubscription::cancel)
            val active =
                selected
                    .create()
                    .record(
                        onGameConstructed = {
                          mark("construction.end")
                          measure("construction", "load.start", "construction.end")
                        },
                        onReplayCompleted = {
                          mark("replay.end")
                          measure("authored-replay", "construction.end", "replay.end")
                        },
                    )
            val logEvents = active.world.visibleLogEvents()
            selectablePositions =
                selectablePositionIndices(active.positions, logEvents.map(ChangeEvent::ordinal))
            mark("preparation.end")
            measure("recording-and-log-positions", "replay.end", "preparation.end")
            measurePhase("initial-seek") { active.seek(selectablePositions.first()) }
            recording = active
            recordingName = selected.name
            selectedPlayerIndex = 0
            mapSubscriptions = measurePhase("render.map") { renderMap(active) }
            measurePhase("render.player-tabs") {
              renderPlayerTabs(active) { index ->
                selectedPlayerIndex = index
                updatePosition(active, scrollLog = false)
              }
            }
            measurePhase("render.log") {
              renderLog(active, logEvents, selectablePositions) { index ->
                showPosition(active, index)
              }
            }
            updatePosition(active, scrollLog = true)
            mark("load.end")
            measure("load-total", "load.start", "load.end")
            gameSelect.disabled = false
          } catch (failure: Throwable) {
            recording = null
            status.textContent = "Could not replay ${selected.name}: ${failure.message}"
            gameSelect.disabled = false
          }
        },
        0,
    )
  }

  gameSelect.addEventListener("change", { loadSelectedGame() })
  window.addEventListener(
      "keydown",
      keydown@{ rawEvent ->
        val event = rawEvent as KeyboardEvent
        if (event.key != "ArrowUp" && event.key != "ArrowDown") return@keydown
        if ((event.target as? Element)?.tagName == "SELECT") return@keydown
        val active = recording ?: return@keydown
        val delta = if (event.key == "ArrowUp") -1 else 1
        val current = selectablePositions.indexOf(active.positionIndex)
        val next = (current + delta).coerceIn(selectablePositions.indices)
        if (next != current) {
          event.preventDefault()
          showPosition(active, selectablePositions[next])
        }
      },
  )
  positionLabel.textContent = "Built ${document.lastModified}"
}

private fun clearBenchmarkEntries() {
  val phases =
      listOf(
          "construction",
          "authored-replay",
          "recording-and-log-positions",
          "initial-seek",
          "render.map",
          "render.player-tabs",
          "render.log",
          "render.player-tabs-update",
          "render.dashboard",
          "render.cards",
          "render.log-state",
          "render.log-scroll",
          "load-total",
      )
  phases.forEach { phase ->
    window.performance.asDynamic().clearMeasures("$BENCHMARK_PREFIX:$phase")
  }
  listOf("load.start", "construction.end", "replay.end", "preparation.end", "load.end").forEach {
      name ->
    window.performance.asDynamic().clearMarks("$BENCHMARK_PREFIX:$name")
  }
}

private fun mark(name: String) {
  window.performance.asDynamic().mark("$BENCHMARK_PREFIX:$name")
}

private fun measure(name: String, start: String, end: String) {
  window.performance
      .asDynamic()
      .measure(
          "$BENCHMARK_PREFIX:$name",
          "$BENCHMARK_PREFIX:$start",
          "$BENCHMARK_PREFIX:$end",
      )
}

private inline fun <T> measurePhase(name: String, block: () -> T): T {
  val start = "$name.start"
  val end = "$name.end"
  mark(start)
  return try {
    block()
  } finally {
    mark(end)
    measure(name, start, end)
    window.performance.asDynamic().clearMarks("$BENCHMARK_PREFIX:$start")
    window.performance.asDynamic().clearMarks("$BENCHMARK_PREFIX:$end")
  }
}

private fun renderPlayerTabs(recording: GameRecording, onSelect: (Int) -> Unit) {
  val game = recording.world
  val tabs = checkNotNull(document.getElementById("player-tabs"))
  tabs.innerHTML = ""
  game.actors.filterIsInstance<Player>().forEachIndexed { index, player ->
    val name = game.vocabulary.displayName(player.className)
    val tab = document.createElement("button")
    tab.className = "player-tab player-${playerColor(name)}"
    tab.setAttribute("type", "button")
    tab.setAttribute("role", "tab")
    tab.setAttribute("data-player-index", index.toString())
    tab.textContent = name
    tab.addEventListener("click", { onSelect(index) })
    tabs.appendChild(tab)
  }
}

private fun updatePlayerTabs(recording: GameRecording, selectedPlayerIndex: Int) {
  val tabs = checkNotNull(document.getElementById("player-tabs"))
  for (index in 0 until tabs.children.length) {
    val tab = tabs.children.item(index) ?: continue
    val selected = tab.getAttribute("data-player-index")?.toInt() == selectedPlayerIndex
    if (selected) tab.classList.add("active") else tab.classList.remove("active")
    tab.setAttribute("aria-selected", selected.toString())
  }
  val player = recording.world.actors.filterIsInstance<Player>()[selectedPlayerIndex]
  val name = recording.world.vocabulary.displayName(player.className)
  document.getElementById("dashboard-panel")?.className =
      "dashboard-panel player-${playerColor(name)}"
}

private fun renderDashboard(recording: GameRecording, player: Player) {
  val game = recording.world
  val tfm = game.tfm(player)

  fun setValue(name: String, value: Any?) {
    document.querySelector("[data-stat='$name']")?.textContent = value?.toString() ?: "—"
  }

  fun countIfLoaded(type: String): Int =
      try {
        tfm.count(type)
      } catch (_: ExpressionException) {
        0
      }

  val corporation =
      playedCards(game, player).firstOrNull { cardImageDirectory(it) == "corporations" }
  setValue("player-name", game.vocabulary.displayName(player.className))
  setValue("corporation-name", corporation?.let { game.vocabulary.displayName(it.className) })
  setValue("phase", tfm.list("Phase").singleOrNull()?.toString()?.removeSuffix("Phase") ?: "—")
  setValue("terraform-rating", countIfLoaded("TerraformRating"))
  setValue("cards", countIfLoaded("ProjectCard"))

  linkedMapOf(
          "megacredit" to "MC",
          "steel" to "Steel",
          "titanium" to "Titanium",
          "plant" to "Plant",
          "energy" to "Energy",
          "heat" to "Heat",
      )
      .forEach { (name, type) ->
        setValue("$name-stock", countIfLoaded(type))
        val production = tfm.production(dev.martianzoo.pets.ast.ClassName.cn(type))
        setValue("$name-production", if (production > 0) "+$production" else production)
      }

  linkedMapOf(
          "building" to "BuildingTag",
          "space" to "SpaceTag",
          "science" to "ScienceTag",
          "power" to "PowerTag",
          "earth" to "EarthTag",
          "jovian" to "JovianTag",
          "venus" to "VenusTag",
          "plant" to "PlantTag",
          "microbe" to "MicrobeTag",
          "animal" to "AnimalTag",
          "city" to "CityTag",
          "event" to "PlayedEvent",
      )
      .forEach { (name, type) ->
        val loaded = countIfLoaded("Class<$type>") > 0
        val element = document.querySelector("[data-tag='$name']")
        if (loaded) element?.removeAttribute("hidden") else element?.setAttribute("hidden", "")
        val count =
            if (type == "PlayedEvent") playedEventCards(game, player).size else countIfLoaded(type)
        setValue("$name-tag", if (loaded) count else 0)
      }
}

private fun renderCards(recording: GameRecording, player: Player) {
  val game = recording.world
  val container = checkNotNull(document.getElementById("played-cards"))
  container.innerHTML = ""
  val cards = playedCards(game, player)
  val events = playedEventCards(game, player)
  if (cards.isEmpty() && events.isEmpty()) {
    val empty = document.createElement("p")
    empty.className = "empty-cards"
    empty.textContent = "No cards in play at this point."
    container.appendChild(empty)
    return
  }

  fun appendCardImage(
      directory: String,
      cardName: dev.martianzoo.pets.ast.ClassName,
      resourceCount: Pair<dev.martianzoo.pets.ast.ClassName, Int>? = null,
      actionUsed: Boolean = false,
  ) {
    val slot = document.createElement("div")
    slot.className = "played-card-slot $directory-card-slot"
    val image = document.createElement("img")
    val displayName = game.vocabulary.displayName(cardName)
    image.className = "played-card"
    image.setAttribute("src", "images/$directory/$cardName.png")
    image.setAttribute("alt", displayName)
    image.setAttribute("title", displayName)
    slot.appendChild(image)
    resourceCount?.let { (resourceType, count) ->
      val counter = document.createElement("div")
      counter.className = "card-resources-counter"
      counter.setAttribute(
          "title",
          "$count ${game.vocabulary.displayName(resourceType)} on $displayName",
      )
      val number = document.createElement("span")
      number.className = "card-resources-counter-number"
      number.textContent = count.toString()
      val resource = document.createElement("img")
      resource.className = "card-resource-icon"
      resource.setAttribute("src", "images/resources/$resourceType.png")
      resource.setAttribute("alt", "")
      counter.appendChild(number)
      counter.appendChild(resource)
      slot.appendChild(counter)
    }
    if (actionUsed) {
      val marker = document.createElement("span")
      val color = playerColor(game.vocabulary.displayName(player.className))
      marker.className = "action-used-marker player-$color"
      marker.setAttribute("role", "img")
      marker.setAttribute("aria-label", "Action used")
      marker.setAttribute("title", "Action used this generation")
      slot.appendChild(marker)
    }
    container.appendChild(slot)
  }

  cards.forEach { card ->
    val directory = cardImageDirectory(card) ?: return@forEach
    appendCardImage(
        directory,
        card.className,
        cardResourceCount(game, player, card),
        hasActionUsedMarker(game, player, card),
    )
  }
  if (events.isNotEmpty()) {
    val divider = document.createElement("div")
    divider.className = "event-cards-divider"
    divider.textContent = "Played events"
    container.appendChild(divider)
    events.forEach { appendCardImage("projects", it) }
  }
}

private fun renderLog(
    recording: GameRecording,
    events: List<ChangeEvent>,
    selectablePositions: List<Int>,
    onSeek: (Int) -> Unit,
) {
  val game = recording.world
  val log = checkNotNull(document.getElementById("game-log"))
  log.innerHTML = ""
  var selectableIndex = 0

  fun appendStopsThrough(ordinal: Int) {
    while (
        selectableIndex < selectablePositions.size &&
            recording.positions[selectablePositions[selectableIndex]].ordinal <= ordinal
    ) {
      val index = selectablePositions[selectableIndex++]
      val checkpoint = recording.positions[index]
      val stop = document.createElement("button")
      stop.className = "timeline-stop"
      stop.setAttribute("type", "button")
      stop.setAttribute("data-position", index.toString())
      stop.textContent = "event ${checkpoint.ordinal}"
      stop.setAttribute("aria-label", "Go to timeline position ${index + 1}")
      stop.addEventListener("click", { onSeek(index) })
      log.appendChild(stop)
    }
  }

  events.forEach { event ->
    appendStopsThrough(event.ordinal)
    val line = document.createElement("div")
    line.className = "log-line"
    line.setAttribute("data-ordinal", event.ordinal.toString())
    line.textContent = game.vocabulary.renderPets(event)
    log.appendChild(line)
  }
  appendStopsThrough(Int.MAX_VALUE)
}

private fun updateLogState(recording: GameRecording) {
  val log = checkNotNull(document.getElementById("game-log"))
  val checkpoint = recording.positions[recording.positionIndex]
  for (index in 0 until log.children.length) {
    val child = log.children.item(index) ?: continue
    child.getAttribute("data-ordinal")?.toInt()?.let { ordinal ->
      if (ordinal >= checkpoint.ordinal) child.classList.add("future")
      else child.classList.remove("future")
    }
    child.getAttribute("data-position")?.toInt()?.let { position ->
      if (position == recording.positionIndex) child.classList.add("active")
      else child.classList.remove("active")
    }
  }
}

private fun scrollActiveLogStop() {
  val active = document.querySelector(".timeline-stop.active") as? HTMLElement ?: return
  active.asDynamic().scrollIntoView(js("({block: 'nearest'})"))
}

private fun renderMap(recording: GameRecording): List<CountSubscription> {
  val game = recording.world
  val map = mapDefinition(game.reader)
  document.getElementById("mars-map")?.innerHTML = buildString {
    append("<svg viewBox='0 0 1000 1000' role='img' aria-labelledby='map-title'>")
    append("<title id='map-title'>${map.className} map</title>")
    map.areas.forEach { area -> append(areaBaseSvg(area)) }
    append("</svg>")
  }

  return map.areas.map { area ->
    val type = game.reader.resolve(TILE.of(area.className))
    game.components.listenToCount(type, game.reader) { renderAreaState(recording, area) }
  }
}

private fun areaBaseSvg(area: AreaDefinition): String {
  val (centerX, centerY) = areaCenter(area)
  val halfWidth = 50.2
  val radius = 58.0
  val halfStep = radius / 2
  val points =
      listOf(
              centerX to centerY - radius,
              centerX + halfWidth to centerY - halfStep,
              centerX + halfWidth to centerY + halfStep,
              centerX to centerY + radius,
              centerX - halfWidth to centerY + halfStep,
              centerX - halfWidth to centerY - halfStep,
          )
          .joinToString(" ") { (x, y) -> "$x,$y" }
  val kind = area.kind.toString().removeSuffix("Area").lowercase()
  return "<polygon class='map-space $kind' points='$points'/>" +
      "<g id='map-state-${area.row}-${area.column}'></g>"
}

private fun renderAreaState(recording: GameRecording, area: AreaDefinition) {
  val game = recording.world
  val reader = game.reader
  val target = document.getElementById("map-state-${area.row}-${area.column}") ?: return
  val (centerX, centerY) = areaCenter(area)
  val tile = reader.getComponents(reader.resolve(TILE.of(area.className))).singleOrNull()
  target.innerHTML =
      if (tile == null) {
        emptyAreaSvg(area, centerX, centerY)
      } else {
        val owner =
            tile.expressionFull.arguments
                .firstOrNull { Player.isValid(it.className) }
                ?.className
                ?.let { ownerName ->
                  game.actors
                      .filterIsInstance<Player>()
                      .firstOrNull { it.className == ownerName }
                      ?.let { game.vocabulary.displayName(it.className) }
                }
                ?.let(::playerColor)
        buildString {
          val imageBox =
              if (tile.className.toString() == "GreeneryTile") {
                "x='${centerX - 46.5}' y='${centerY - 53.5}' width='93' height='107'"
              } else {
                "x='${centerX - 59}' y='${centerY - 59}' width='118' height='118'"
              }
          append(
              "<image class='map-tile' href='images/maps-tiles/${tile.className}.png' " +
                  "$imageBox preserveAspectRatio='xMidYMid meet'/>"
          )
          owner?.let {
            append(
                "<rect class='owner-cube player-$it' x='${centerX + 21}' " +
                    "y='${centerY + 17}' width='15' height='15' rx='2'/>"
            )
          }
        }
      }
}

private fun emptyAreaSvg(area: AreaDefinition, centerX: Double, centerY: Double): String =
    buildString {
      val kind = area.kind.toString().removeSuffix("Area").lowercase()
      if (kind == "volcanic") {
        append(
            "<path class='volcano-marker' d='M ${centerX - 17.2},${centerY + 29.6} " +
                "L ${centerX - 6.8},${centerY + 8.8} L ${centerX - 1.2},${centerY + 16} " +
                "L ${centerX + 6.8},${centerY + 5.6} L ${centerX + 17.2},${centerY + 29.6} Z'/>"
        )
      }
      if (kind == "noctis") {
        append("<text class='noctis-label' x='$centerX' y='${centerY + 21}'>Noctis</text>")
        append("<text class='noctis-label' x='$centerX' y='${centerY + 38}'>City</text>")
      }
      val bonusX = centerX - 45.7
      val bonusY = centerY - 23.0
      area.code.drop(1).forEachIndexed { index, bonus ->
        val asset =
            when (bonus) {
              'P' -> "images/resources/Plant.png"
              'S' -> "images/resources/Steel.png"
              'T' -> "images/resources/Titanium.png"
              'C' -> "images/other/ProjectCard.png"
              'H' -> "images/resources/Heat.png"
              else -> null
            }
        asset?.let {
          val x = bonusX + index * 25.0
          append(
              "<image class='bonus-icon' href='$it' " +
                  "x='$x' y='$bonusY' width='25' height='25'/>"
          )
        }
      }
    }

private fun areaCenter(area: AreaDefinition): Pair<Double, Double> =
    500.0 + 107.0 * (area.column - area.row / 2.0 - 2.5) to 125.0 + 93.5 * (area.row - 1)
