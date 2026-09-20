package dev.martianzoo.tfm.web.gameviewer

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.displayName
import dev.martianzoo.state.ComponentGraph.CountSubscription
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.GameRecordingJson
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.TfmClasses.MC
import dev.martianzoo.tfm.canon.TfmClasses.TILE
import dev.martianzoo.tfm.fake.FakeCanon
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.KeyboardEvent

private const val BENCHMARK_PREFIX = "game-viewer"
private val classWord = Regex("[A-Za-z][A-Za-z0-9_]*")

public fun main() {
  val gameSelect = document.getElementById("game-select") as HTMLSelectElement
  val status = checkNotNull(document.getElementById("status"))
  val positionLabel = checkNotNull(document.getElementById("position-label"))
  var recording: GameRecording.Playback? = null
  var savedGames = emptyList<SavedGame>()
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

  gameSelect.disabled = true

  fun updatePosition(active: GameRecording.Playback, scrollLog: Boolean) {
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

  fun showPosition(active: GameRecording.Playback, index: Int, scrollLog: Boolean = true) {
    if (index !in active.positions.indices) return
    active.seek(index)
    updatePosition(active, scrollLog)
  }

  fun loadSelectedGame() {
    if (gameSelect.value.isEmpty()) return
    val selected = savedGames[gameSelect.value.toInt()]
    gameSelect.disabled = true
    status.textContent = "Loading ${selected.name}…"
    window
        .fetch(selected.resourcePath)
        .then { response ->
          if (!response.ok) error("HTTP ${response.status}")
          response.text()
        }
        .then { text ->
          try {
            clearBenchmarkEntries()
            mark("load.start")
            mapSubscriptions.forEach(CountSubscription::cancel)
            val config = GameRecordingJson.config(text)
            val catalog: TfmCatalog =
                if (cn("FakeStuffBundle") in config.includedClassNames) {
                  TfmCatalog.compose(Canon, FakeCanon)
                } else {
                  Canon
                }
            val premise = catalog.gamePremise(config)
            val active = GameRecordingJson.decode(text, premise).open()
            val logEvents =
                visibleLogEvents(active.world.events.changesSinceSetup(), active.world.reader)
            selectablePositions =
                selectablePositionIndices(active.positions, logEvents.map(ChangeEvent::ordinal))
            mark("preparation.end")
            measure("recording-and-log-positions", "load.start", "preparation.end")
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
        }
        .catch { failure ->
          recording = null
          status.textContent = "Could not load ${selected.name}: ${failure.message}"
          gameSelect.disabled = false
        }
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

  window
      .fetch("games/index.txt")
      .then { response ->
        if (!response.ok) error("HTTP ${response.status}")
        response.text()
      }
      .then { text ->
        savedGames = SavedGames.fromIndex(text)
        savedGames.forEachIndexed { index, savedGame ->
          val option = document.createElement("option")
          option.setAttribute("value", index.toString())
          option.textContent = savedGame.name
          gameSelect.appendChild(option)
        }
        gameSelect.disabled = savedGames.isEmpty()
        status.textContent =
            if (savedGames.isEmpty()) "No replay test event logs were built."
            else "Choose a replay test."
      }
      .catch { failure ->
        status.textContent = "Could not discover replay tests: ${failure.message}"
      }
}

private fun clearBenchmarkEntries() {
  val phases =
      listOf(
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
  listOf("load.start", "preparation.end", "load.end").forEach { name ->
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

private fun almanacHref(className: ClassName): String = "/classviewer/#$className"

private fun configureClassLink(element: Element, className: ClassName) {
  element.setAttribute("href", almanacHref(className))
  element.setAttribute("target", "_blank")
  element.setAttribute("rel", "noopener noreferrer")
}

private fun classLink(className: ClassName, label: String = className.toString()): Element =
    document.createElement("a").apply {
      configureClassLink(this, className)
      this.className = "almanac-link"
      textContent = label
    }

private fun appendClassLinkedText(
    container: Element,
    text: String,
    classesByName: Map<String, ClassName>,
) {
  var offset = 0
  classWord.findAll(text).forEach { match ->
    val className = classesByName[match.value] ?: return@forEach
    if (match.range.first > offset) {
      container.appendChild(document.createTextNode(text.substring(offset, match.range.first)))
    }
    container.appendChild(classLink(className))
    offset = match.range.last + 1
  }
  if (offset < text.length) container.appendChild(document.createTextNode(text.substring(offset)))
}

private fun svgClassLink(className: ClassName, content: String): String =
    "<a class='almanac-link' href='${almanacHref(className)}' target='_blank' " +
        "rel='noopener noreferrer'>$content</a>"

private fun renderPlayerTabs(recording: GameRecording.Playback, onSelect: (Int) -> Unit) {
  val game = recording.world
  val tabs = checkNotNull(document.getElementById("player-tabs"))
  tabs.innerHTML = ""
  val players = game.actors.filterIsInstance<Player>()
  val playerNames = players.map { displayName(game.reader.catalog, it.className) }
  val playerColors = assignPlayerColors(playerNames)
  players.forEachIndexed { index, player ->
    val name = playerNames[index]
    val tab = document.createElement("button")
    tab.className = "player-tab almanac-link player-${playerColors[index]}"
    tab.setAttribute("type", "button")
    tab.setAttribute("role", "tab")
    tab.setAttribute("data-player-index", index.toString())
    tab.textContent = name
    tab.addEventListener(
        "click",
        {
          onSelect(index)
          window.open(almanacHref(player.className), "_blank", "noopener")
        },
    )
    tabs.appendChild(tab)
  }
}

private fun updatePlayerTabs(
    recording: GameRecording.Playback,
    selectedPlayerIndex: Int,
) {
  val tabs = checkNotNull(document.getElementById("player-tabs"))
  for (index in 0 until tabs.children.length) {
    val tab = tabs.children.item(index) ?: continue
    val selected = tab.getAttribute("data-player-index")?.toInt() == selectedPlayerIndex
    if (selected) tab.classList.add("active") else tab.classList.remove("active")
    tab.setAttribute("aria-selected", selected.toString())
  }
  val playerNames =
      recording.world.actors.filterIsInstance<Player>().map {
        displayName(recording.world.reader.catalog, it.className)
      }
  val playerColors = assignPlayerColors(playerNames)
  document.getElementById("dashboard-panel")?.className =
      "dashboard-panel player-${playerColors[selectedPlayerIndex]}"
}

private fun renderDashboard(recording: GameRecording.Playback, player: Player) {
  val game = recording.world
  val queries = GameQueries(game.reader)

  fun setValue(name: String, value: Any?) {
    document.querySelector("[data-stat='$name']")?.textContent = value?.toString() ?: "—"
  }

  fun setClassValue(name: String, className: ClassName?, value: Any?) {
    val element = document.querySelector("[data-stat='$name']") ?: return
    element.innerHTML = ""
    if (className == null || value == null) {
      element.textContent = "—"
    } else {
      element.appendChild(classLink(className, value.toString()))
    }
  }

  fun countIfLoaded(type: String): Int =
      try {
        queries.count(player, type)
      } catch (_: ExpressionException) {
        0
      }

  val corporation =
      playedCards(game, player).firstOrNull { cardImageDirectory(it) == "corporations" }
  setClassValue(
      "player-name",
      player.className,
      displayName(game.reader.catalog, player.className),
  )
  setClassValue(
      "corporation-name",
      corporation?.className,
      corporation?.let { displayName(game.reader.catalog, it.className) },
  )
  val phase = game.reader.getComponents("Phase").singleOrNull()
  setClassValue(
      "phase",
      phase?.className,
      phase?.toString()?.removeSuffix("Phase"),
  )
  linkedMapOf("terraform-rating" to "TerraformRating", "cards" to "ProjectCard").forEach {
      (name, type) ->
    setValue(name, countIfLoaded(type))
    document.querySelector("[data-stat-icon='$name']")?.let { configureClassLink(it, cn(type)) }
  }

  linkedMapOf(
          "megacredit" to "MC",
          "steel" to "Steel",
          "titanium" to "Titanium",
          "plant" to "Plant",
          "energy" to "Energy",
          "heat" to "Heat",
      )
      .forEach { (name, type) ->
        document.querySelector("[data-resource='$name'] .resource-icon")?.let {
          configureClassLink(it, cn(type))
        }
        setValue("$name-stock", countIfLoaded(type))
        val production = queries.production(player, cn(type))
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
        val iconClass = if (type == "PlayedEvent") cn("EventTag") else cn(type)
        document.querySelector("[data-tag='$name'] .tag-icon")?.let {
          configureClassLink(it, iconClass)
        }
        val loaded = countIfLoaded("Class<$type>") > 0
        val element = document.querySelector("[data-tag='$name']")
        if (loaded) element?.removeAttribute("hidden") else element?.setAttribute("hidden", "")
        val count =
            if (type == "PlayedEvent") playedEventCards(game, player).size else countIfLoaded(type)
        setValue("$name-tag", if (loaded) count else 0)
      }
}

private fun renderCards(recording: GameRecording.Playback, player: Player) {
  val game = recording.world
  val container = checkNotNull(document.getElementById("played-cards"))
  container.innerHTML = ""
  val cards = playedCards(game, player)
  val events = playedEventCards(game, player)
  val players = game.actors.filterIsInstance<Player>()
  val playerNames = players.map { displayName(game.reader.catalog, it.className) }
  val color = assignPlayerColors(playerNames)[players.indexOf(player)]
  if (cards.isEmpty() && events.isEmpty()) {
    val empty = document.createElement("p")
    empty.className = "empty-cards"
    empty.textContent = "No cards in play at this point."
    container.appendChild(empty)
    return
  }

  fun appendCardImage(
      directory: String,
      cardName: ClassName,
      resourceCount: Pair<ClassName, Int>? = null,
      actionUsed: Boolean = false,
  ) {
    val slot = document.createElement("div")
    slot.className = "played-card-slot $directory-card-slot"
    val image = document.createElement("img")
    val cardDisplayName = displayName(game.reader.catalog, cardName)
    image.className = "played-card"
    image.setAttribute("src", "images/$cardName.png")
    image.setAttribute("alt", cardDisplayName)
    image.setAttribute("title", cardDisplayName)
    val imageLink = classLink(cardName)
    imageLink.className = "card-image-link almanac-link"
    imageLink.textContent = ""
    imageLink.appendChild(image)
    slot.appendChild(imageLink)
    resourceCount?.let { (resourceType, count) ->
      val counter = document.createElement("div")
      counter.className = "card-resources-counter"
      counter.setAttribute(
          "title",
          "$count ${displayName(game.reader.catalog, resourceType)} on $cardDisplayName",
      )
      val number = document.createElement("span")
      number.className = "card-resources-counter-number"
      number.textContent = count.toString()
      val resource = document.createElement("img")
      resource.className = "card-resource-icon"
      resource.setAttribute("src", "images/$resourceType.png")
      resource.setAttribute("alt", displayName(game.reader.catalog, resourceType))
      counter.appendChild(number)
      val resourceLink = classLink(resourceType)
      resourceLink.className = "card-resource-link almanac-link"
      resourceLink.textContent = ""
      resourceLink.appendChild(resource)
      counter.appendChild(resourceLink)
      slot.appendChild(counter)
    }
    if (actionUsed) {
      val marker = classLink(cn("ActionUsedMarker"), "")
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
        cardResourceCount(game.reader, player, card),
        hasActionUsedMarker(game.reader, player, card),
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
    recording: GameRecording.Playback,
    events: List<ChangeEvent>,
    selectablePositions: List<Int>,
    onSeek: (Int) -> Unit,
) {
  val log = checkNotNull(document.getElementById("game-log"))
  log.innerHTML = ""
  var selectableIndex = 0
  val classesByName =
      recording.world.reader.classTable.allClasses().associate {
        it.className.toString() to it.className
      }

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
    appendClassLinkedText(line, event.toString(), classesByName)
    log.appendChild(line)
  }
  appendStopsThrough(Int.MAX_VALUE)
}

private fun updateLogState(recording: GameRecording.Playback) {
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

private fun renderMap(recording: GameRecording.Playback): List<CountSubscription> {
  val game = recording.world
  val map = mapDefinition(game.reader)
  document.getElementById("mars-map")?.innerHTML = buildString {
    append("<svg viewBox='${mapViewBox(map.areas)}' role='img' aria-labelledby='map-title'>")
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
  return svgClassLink(
      area.className,
      "<polygon class='map-space $kind' points='$points'><title>${area.className}</title></polygon>",
  ) + "<g id='map-state-${area.row}-${area.column}'></g>"
}

private fun renderAreaState(recording: GameRecording.Playback, area: AreaDefinition) {
  val game = recording.world
  val reader = game.reader
  val target = document.getElementById("map-state-${area.row}-${area.column}") ?: return
  val (centerX, centerY) = areaCenter(area)
  val tile = reader.getComponents(reader.resolve(TILE.of(area.className))).singleOrNull()
  val players = game.actors.filterIsInstance<Player>()
  val playerClassNames = players.mapTo(hashSetOf(), Player::className)
  val playerNames = players.map { displayName(game.reader.catalog, it.className) }
  val playerColors = assignPlayerColors(playerNames)
  target.innerHTML =
      if (tile == null) {
        emptyAreaSvg(area, centerX, centerY)
      } else {
        val owner =
            tile.typeDependencies
                .map { it.boundType }
                .firstOrNull { it.className in playerClassNames }
                ?.className
                ?.let { ownerName ->
                  players
                      .indexOfFirst { it.className == ownerName }
                      .takeIf { it >= 0 }
                      ?.let { ownerName to playerColors[it] }
                }
        buildString {
          val imageBox =
              if (tile.className.toString() == "GreeneryTile") {
                "x='${centerX - 46.5}' y='${centerY - 53.5}' width='93' height='107'"
              } else {
                "x='${centerX - 59}' y='${centerY - 59}' width='118' height='118'"
              }
          append(
              svgClassLink(
                  tile.className,
                  "<image class='map-tile' href='images/${tile.className}.png' " +
                      "$imageBox preserveAspectRatio='xMidYMid meet'>" +
                      "<title>${tile.className}</title></image>",
              )
          )
          owner?.let { (ownerName, ownerColor) ->
            append(
                svgClassLink(
                    ownerName,
                    "<rect class='owner-cube player-$ownerColor' x='${centerX + 21}' " +
                        "y='${centerY + 17}' width='15' height='15' rx='2'>" +
                        "<title>$ownerName</title></rect>",
                )
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
            svgClassLink(
                area.className,
                "<path class='volcano-marker' d='M ${centerX - 17.2},${centerY + 29.6} " +
                    "L ${centerX - 6.8},${centerY + 8.8} L ${centerX - 1.2},${centerY + 16} " +
                    "L ${centerX + 6.8},${centerY + 5.6} " +
                    "L ${centerX + 17.2},${centerY + 29.6} Z'/>",
            )
        )
      }
      if (kind == "noctis") {
        append(
            svgClassLink(
                area.className,
                "<text class='noctis-label' x='$centerX' y='${centerY + 21}'>Noctis</text>" +
                    "<text class='noctis-label' x='$centerX' y='${centerY + 38}'>City</text>",
            )
        )
      }
      val bonusX = centerX - 45.7
      val bonusY = centerY - 23.0
      area.bonus
          ?.instructions
          .orEmpty()
          .flatMap { instruction ->
            val change = instruction as? Change ?: return@flatMap emptyList()
            val count = (change.count as? ActualScalar)?.value ?: return@flatMap emptyList()
            when {
              change.gaining?.className == MC && count in 1..49 ->
                  listOf(Triple("MC/MC${count.toString().padStart(2, '0')}", null, MC))
              change.gaining != null ->
                  List(count) {
                    Triple(
                        change.gaining!!.className.toString(),
                        null,
                        change.gaining!!.className,
                    )
                  }
              change.removing?.className == MC && count in 1..9 ->
                  listOf(Triple("MC/MC-$count", null, MC))
              change.removing?.className == MC -> listOf(Triple(null, "−$count", MC))
              else -> emptyList()
            }
          }
          .forEachIndexed { index, (imageName, label, className) ->
            val x = bonusX + index * 25.0
            if (imageName != null) {
              append(
                  svgClassLink(
                      className,
                      "<image class='bonus-icon' href='images/$imageName.png' " +
                          "x='$x' y='$bonusY' width='25' height='25'>" +
                          "<title>$className</title></image>",
                  )
              )
            } else if (label != null) {
              append(
                  svgClassLink(
                      className,
                      "<text class='bonus-text' x='${x + 12.5}' " +
                          "y='${bonusY + 12.5}'>$label</text>",
                  )
              )
            }
          }
    }

private fun mapViewBox(areas: Iterable<AreaDefinition>): String {
  val centers = areas.map(::areaCenter)
  val radius = 62.0
  val minX = minOf(0.0, centers.minOf { it.first } - radius)
  val maxX = maxOf(1000.0, centers.maxOf { it.first } + radius)
  val minY = minOf(0.0, centers.minOf { it.second } - radius)
  val maxY = maxOf(1000.0, centers.maxOf { it.second } + radius)
  val side = maxOf(maxX - minX, maxY - minY)
  return "${minX - (side - (maxX - minX)) / 2} " +
      "${minY - (side - (maxY - minY)) / 2} $side $side"
}

private fun areaCenter(area: AreaDefinition): Pair<Double, Double> =
    500.0 + 107.0 * (area.column - area.row / 2.0 - 2.5) to 125.0 + 93.5 * (area.row - 1)
