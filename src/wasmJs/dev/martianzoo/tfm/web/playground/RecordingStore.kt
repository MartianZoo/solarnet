package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.GameRecordingJson
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import dev.martianzoo.tfm.web.playground.resources.Res

/** The loaded passive recording and the player-request position shown in the workspace. */
@Stable
internal class RecordingStore {
  data class ActionStop(val generation: Int, val eventOrdinal: Int, val player: String)

  var gameNames by mutableStateOf(emptyList<String>())
    private set

  var gameName by mutableStateOf("")
    private set

  var specimen by mutableStateOf<Specimen?>(null)
    private set

  var error by mutableStateOf<String?>(null)
    private set

  var config by mutableStateOf<GameConfig?>(null)
    private set

  private var playback: GameRecording.Playback? = null
  private var changes = emptyList<ChangeEvent>()
  private var stops = emptyList<Int>()

  var generationStarts by mutableStateOf(emptyList<Int>())
    private set

  var actionStops by mutableStateOf(emptyList<ActionStop>())
    private set

  val eventOrdinal: Int
    get() = playback?.let { it.positions[it.positionIndex].ordinal } ?: 0

  val classTable: ClassTable?
    get() = playback?.world?.reader?.classTable

  val premise: GamePremise?
    get() = playback?.world?.premise

  val stopOrdinals: List<Int>
    get() = playback?.let { active -> stops.map { active.positions[it].ordinal } } ?: emptyList()

  suspend fun discover() {
    try {
      gameNames =
          Res.readBytes("files/games/index.txt")
              .decodeToString()
              .lineSequence()
              .map(String::trim)
              .filter(String::isNotEmpty)
              .toList()
      require(gameNames.isNotEmpty()) { "No replay recordings were built." }
      load(if ("OtbGame20260912Test" in gameNames) "OtbGame20260912Test" else gameNames.first())
    } catch (failure: Throwable) {
      error = "Could not load replay recordings: ${failure.message}"
    }
  }

  suspend fun load(name: String) {
    require(name in gameNames)
    try {
      val text = Res.readBytes("files/games/$name.json").decodeToString()
      val document = GameRecordingJson.parse(text)
      val catalog: TfmCatalog =
          if (cn("FakeStuffBundle") in document.config.includedClassNames) {
            TfmCatalog(Canon, FakeCanon)
          } else {
            Canon
          }
      val recording = document.decode(catalog.gamePremise(document.config))
      val active = recording.open()
      val allChanges = active.world.events.changesSinceSetup()
      val recordedChanges = allChanges
      val actionSignals = setOf(cn("NewTurn"), cn("SecondAction"))
      val generationEvents = recordedChanges.filter {
        it.change is ComponentChange.Gain && it.change.gaining?.className == cn("Generation")
      }
      val performedActionTriggers =
          recordedChanges
              .filter {
                it.cause?.context?.className == cn("ActionPhase") &&
                    it.change.gaining != null &&
                    it.change.gaining?.className != cn("Pass")
              }
              .mapNotNull { it.cause?.triggerEvent }
              .toSet()
      val recordedActions = recordedChanges.mapNotNull { event ->
        val gained = event.change.gaining ?: return@mapNotNull null
        if (event.ordinal !in performedActionTriggers || gained.className !in actionSignals)
            return@mapNotNull null
        val player = gained.owningPlayer?.className?.toString() ?: return@mapNotNull null
        ActionStop(
            generationEvents.count { it.ordinal < event.ordinal },
            event.ordinal,
            player,
        )
      }
      val recordedEvents = recording.events
      var nextEvent = 0
      var pendingTasks = 0
      val requestPositions =
          active.positions.indices.filter { index ->
            while (nextEvent < active.positions[index].ordinal) {
              when (recordedEvents[nextEvent++]) {
                is TaskAddedEvent -> pendingTasks++
                is TaskRemovedEvent -> pendingTasks--
                else -> Unit
              }
            }
            pendingTasks > 0 || index == active.positions.lastIndex
          }
      val selectable = requestPositions
      val initialEvent = if (name == "OtbGame20260912Test") 12896 else Int.MAX_VALUE
      val initialPosition =
          selectable.firstOrNull { active.positions[it].ordinal > initialEvent }
              ?: selectable.last()
      active.seek(initialPosition)
      val display = liveSpecimen(name, active, allChanges)
      playback = active
      changes = allChanges
      stops = selectable
      generationStarts = generationEvents.map(ChangeEvent::ordinal)
      actionStops = recordedActions
      gameName = name
      config = document.requestedConfig
      specimen = display
      error = null
    } catch (failure: Throwable) {
      error = "Could not replay $name: ${failure.message}"
    }
  }

  fun seekToEvent(ordinal: Int) {
    val active = playback ?: return
    val position = stops.firstOrNull { active.positions[it].ordinal > ordinal } ?: stops.last()
    seek(position)
  }

  fun seekToStop(ordinal: Int) {
    val active = playback ?: return
    stops.firstOrNull { active.positions[it].ordinal == ordinal }?.let(::seek)
  }

  fun isVisibleEvent(ordinal: Int): Boolean =
      specimen?.events?.binarySearchBy(ordinal) { it.ordinal }?.let { it >= 0 } ?: false

  fun hiddenEventDescription(ordinal: Int): String =
      playback
          ?.world
          ?.events
          ?.entriesSinceSetup()
          ?.firstOrNull { it.ordinal == ordinal }
          ?.toString() ?: "Event $ordinal is not in this recording"

  fun stepStop(direction: Int): Boolean {
    val active = playback ?: return false
    val current = stops.indexOf(active.positionIndex)
    val next = current + direction
    if (next !in stops.indices) return false
    seek(stops[next])
    return true
  }

  fun stepAction(direction: Int): Boolean {
    val active = playback ?: return false
    val actionPositions = actionStops.mapNotNull { action ->
      stops.firstOrNull { active.positions[it].ordinal > action.eventOrdinal }
    }
    val target =
        if (direction < 0) {
          actionPositions.lastOrNull { active.positions[it].ordinal < eventOrdinal }
        } else {
          actionPositions.firstOrNull { active.positions[it].ordinal > eventOrdinal }
        } ?: return false
    seek(target)
    return true
  }

  fun seekToStart() {
    seek(stops.firstOrNull() ?: return)
  }

  private fun seek(position: Int) {
    val active = playback ?: return
    active.seek(position)
    specimen = liveSpecimen(gameName, active, changes)
  }
}
