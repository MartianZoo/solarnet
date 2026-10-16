package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import dev.martianzoo.tfm.web.playground.resources.Res
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The game shown in the workspace, either a passive recording or the live solo game, and the
 * completed position being viewed. A live game away from its latest position is in history mode.
 */
@Stable
internal class RecordingStore {
  data class ActionStop(
      val generation: Int,
      val eventOrdinal: Int,
      val player: String,
      val summary: String,
  )

  /** A shown game's completed positions and the reads at the current one. */
  private interface Shown {
    val reader: GameReader
    val events: EventLog
    val tasks: TaskQueue
    val premise: GamePremise
    val positions: List<Checkpoint>
    val positionIndex: Int

    fun seek(index: Int)
  }

  private class Recorded(private val playback: GameRecording.Playback) : Shown {
    override val reader = playback.world.reader
    override val events = playback.world.events
    override val tasks = playback.world.tasks
    override val premise = playback.world.premise
    override val positions = playback.positions
    override val positionIndex
      get() = playback.positionIndex

    override fun seek(index: Int) = playback.seek(index)
  }

  private class Live(val session: PlaySession) : Shown {
    override val reader
      get() = session.world.reader

    override val events
      get() = session.world.events

    override val tasks
      get() = session.world.tasks

    override val premise = session.premise
    override val positions
      get() = session.stops

    override val positionIndex
      get() = positions.indexOf(session.world.timeline.checkpoint())

    override fun seek(index: Int) = session.seek(positions[index])
  }

  var gameNames by mutableStateOf(emptyList<String>())
    private set

  var gameName by mutableStateOf("")
    private set

  /** Changes only when a newly opened recording or session replaces the displayed game. */
  var gameRevision by mutableIntStateOf(0)
    private set

  var specimen by mutableStateOf<Specimen?>(null)
    private set

  var error by mutableStateOf<String?>(null)
    private set

  var config by mutableStateOf<GameConfig?>(null)
    private set

  /** The live game's current prompt, or null when a recording is shown. */
  var prompt by mutableStateOf<Prompt?>(null)
    private set

  /** Why the live game refused the last input, if it did. */
  var problem by mutableStateOf<String?>(null)
    private set

  /** Whether a live game is shown away from its latest position. */
  var historyMode by mutableStateOf(true)
    private set

  var bootstrapEffects by mutableStateOf(emptySet<Effect>())
    private set

  private var shown: Shown? = null
  private var changes = emptyList<ChangeEvent>()
  private var balances = emptyMap<Int, Int>()
  private var eventInstructions = emptyMap<Int, String>()

  var focusedEvent by mutableStateOf<Int?>(null)
    private set

  var navigationRevision by mutableIntStateOf(0)
    private set

  var generationStarts by mutableStateOf(emptyList<Int>())
    private set

  var actionStops by mutableStateOf(emptyList<ActionStop>())
    private set

  var includeAdmin by mutableStateOf(true)
  var includeImaginary by mutableStateOf(false)

  private var turnStops = emptySet<Int>()

  val visibleEvents: List<Specimen.LoggedChange>
    get() =
        specimen?.events.orEmpty().filter { event ->
          event.ordinal == focusedEvent ||
              ((includeAdmin || event.actor != "Admin") && (includeImaginary || event.real))
        }

  /** First decision in each gap between visible rows; null denotes the gap after the last row. */
  val visibleStops: Map<Int?, Int>
    get() = collapsedStops(stopOrdinals, visibleEvents.map { it.ordinal })

  val eventOrdinal: Int
    get() = shown?.let { it.positions[it.positionIndex].ordinal } ?: 0

  val classTable: ClassTable?
    get() = shown?.reader?.classTable

  val premise: GamePremise?
    get() = shown?.premise

  /** The shuffle seed of the live game's decks. */
  val seed: Int?
    get() = (shown as? Live)?.session?.seed

  val playing: Boolean
    get() = shown is Live

  var stopOrdinals by mutableStateOf(emptyList<Int>())
    private set

  /** Lists the full game files produced by the replay tests without selecting a game. */
  suspend fun discover() {
    try {
      val index =
          Json.parseToJsonElement(Res.readBytes("files/games/index.json").decodeToString())
              .jsonArray
      descriptions.clear()
      for (entry in index) {
        val fields = entry.jsonObject
        descriptions[fields.getValue("name").jsonPrimitive.content] =
            fields.getValue("config").jsonPrimitive.content to
                fields.getValue("scores").jsonPrimitive.contentOrNull
      }
      gameNames = descriptions.keys.toList()
      require(gameNames.isNotEmpty()) { "No replay recordings were built." }
    } catch (failure: Throwable) {
      error = "Could not list replay recordings: ${failure.message}"
    }
  }

  private val descriptions = mutableMapOf<String, Pair<String, String?>>()

  /** Precomputed configuration and completed scores; never opens the recording. */
  fun describe(name: String): Pair<String, String?> = descriptions.getValue(name)

  private suspend fun readGame(name: String): Pair<GameConfig, GameRecording.Playback> {
    require(name in gameNames)
    val document = GameFileJson.parse(Res.readBytes("files/games/$name.json").decodeToString())
    val catalog =
        if (document.config.includedClassNames.any { it in FakeCanon.allClassNames }) {
          TfmCatalog(Canon, FakeCanon)
        } else Canon
    return document.config to document.open(catalog)
  }

  var loading by mutableStateOf(false)
    private set

  var playbackMessage by mutableStateOf<String?>(null)
    private set

  /** Opens saved events and previews the history; engine verification belongs to replay tests. */
  suspend fun load(name: String) {
    if (loading) return
    loading = true
    playbackMessage = "Loading recording…"
    try {
      val (loadedConfig, active) = readGame(name)
      val recorded = Recorded(active)
      config = loadedConfig
      open(name.removeSuffix(".json"), recorded)
      recorded.seek(0)
      redraw()
      gameRevision++
      playbackMessage = "Playing recorded history…"
      val last = recorded.positions.lastIndex
      val started = TimeSource.Monotonic.markNow()
      // Use elapsed time so expensive frames skip ahead instead of lengthening the preview.
      do {
        delay(50)
        val fraction = (started.elapsedNow().inWholeMilliseconds / 5000.0).coerceAtMost(1.0)
        recorded.seek((last * fraction).toInt())
        navigationRevision++
        redraw()
      } while (recorded.positionIndex < last)
      val winners =
          recorded.reader
              .getComponents(cn("Victory").expression)
              .elements
              .mapNotNull { it.typeDependencies.firstOrNull()?.boundType?.className?.toString() }
              .distinct()
      val result =
          when (winners.size) {
            0 -> "Recording ended."
            1 -> "${winners.single()} wins."
            else -> "${winners.joinToString(" and ")} tie."
          }
      playbackMessage = "$result Click \"Gen 1\" to return to the start of the game."
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (failure: Throwable) {
      playbackMessage = null
      error = "Could not load $name: ${failure.message}"
    } finally {
      loading = false
    }
  }

  /** Starts a live solo game; a failure leaves the shown game in place and reports why. */
  fun play(gameConfig: GameConfig, seed: Int? = null): Boolean {
    if (loading) return false
    playbackMessage = null
    try {
      val session = PlaySession(Canon.gamePremise(gameConfig), seed ?: Random.nextInt())
      config = gameConfig
      open("Solo game", Live(session))
      gameRevision++
      return true
    } catch (failure: Throwable) {
      error = "Could not start the game: ${failure.message}"
      return false
    }
  }

  /** Reads the whole log of [game] at its latest position and shows it there. */
  private fun open(name: String, game: Shown) {
    val entries = game.events.entriesSince(Checkpoint(0))
    val measuredBalances = eventBalances(game.premise, entries)
    bootstrapEffects = bootstrapEffects(game.premise, entries)
    val live = (game as? Live)?.session
    stopOrdinals =
        if (live == null) game.positions.map { it.ordinal }
        else {
          (listOf(live.start) + live.agents.taskLog.selectionCheckpoints() + game.positions.last())
              .filter { it in game.positions && it.ordinal >= live.start.ordinal }
              .map { it.ordinal }
              .distinct()
              .sorted()
        }
    val pending = mutableMapOf<Task.TaskId, Task>()
    val stops = stopOrdinals.toSet()
    val turns = mutableSetOf<Int>()
    fun rememberTurn(ordinal: Int) {
      if (ordinal !in stops) return
      if (
          pending.values.any { task ->
            val cause = task.cause?.triggerEvent?.let { entries[it] as? ChangeEvent }
            !task.deferred &&
                task.assignee is Player &&
                (cause?.change?.gaining?.className == cn("OfferAction") ||
                    cause?.change?.removing?.className == cn("SecondAction"))
          }
      )
          turns += ordinal
    }
    for (event in entries) {
      rememberTurn(event.ordinal)
      when (event) {
        is TaskAddedEvent -> pending[event.task.id] = event.task
        is TaskEditedEvent -> pending[event.task.id] = event.task
        is TaskRemovedEvent -> pending.remove(event.task.id)
        else -> Unit
      }
    }
    rememberTurn(entries.size)
    turnStops = turns
    val allChanges = game.events.changesSince(Checkpoint(0))
    val generationEvents = allChanges.filter {
      it.change is ComponentChange.Gain && it.change.gaining?.className == cn("Generation")
    }
    val table = game.reader.classTable
    val standardAction = table.getClass(cn("StandardAction"))
    val setupCards = listOf("CorporationCard", "PreludeCard").map { table.getClass(cn(it)) }
    val chosenActions = allChanges.filter { event ->
      val gained = event.change.gaining
      val setupPlay =
          gained?.className == cn("PlayCard") &&
              gained.type.typeDependencies.any { dependency ->
                dependency.boundType.representedClass?.let { card ->
                  setupCards.any(card::isSubtypeOf)
                } == true
              }
      setupPlay ||
          (event.cause?.context?.className == cn("InRound") &&
              (gained?.type?.rootClass?.isSubtypeOf(standardAction) == true ||
                  gained?.className == cn("DoRequiredActions")))
    }
    actionStops = chosenActions.mapIndexed { index, event ->
      val end = chosenActions.getOrNull(index + 1)?.ordinal ?: Int.MAX_VALUE
      ActionStop(
          generationEvents.count { it.ordinal < event.ordinal },
          event.ordinal,
          requireNotNull(event.change.gaining?.owningPlayer).className.toString(),
          actionSummary(
              event,
              allChanges.filter { it.ordinal > event.ordinal && it.ordinal < end },
              game.premise.catalog,
          ),
      )
    }
    eventInstructions = allChanges.associate { event ->
      val change = event.change
      event.ordinal to
          (change.gaining?.let { "+${if (change.count == 1) "" else "${change.count} "}$it" }
              ?: change.toString())
    }
    generationStarts = generationEvents.map(ChangeEvent::ordinal)
    changes = allChanges
    balances = measuredBalances
    shown = game
    gameName = name
    focusedEvent = null
    navigationRevision++
    error = null
    redraw()
  }

  /** Rebuilds the display at the shown game's current position. */
  private fun redraw() {
    val game = shown ?: return
    val live = (game as? Live)?.session
    val display =
        liveSpecimen(
            gameName,
            game.reader,
            game.events,
            game.tasks,
            eventOrdinal,
            changes,
            balances,
        )
    specimen =
        if (live == null) display
        else
            display.copy(
                players =
                    display.players.map { player ->
                      if (player.name != live.player.className.toString()) player
                      else player.copy(hand = live.held(HAND), selecting = live.held(SELECTING))
                    }
            )
    historyMode = live == null || !live.atTail
    prompt = live?.prompt()
    problem = live?.problem
  }

  fun seekToEvent(ordinal: Int) {
    val game = shown ?: return
    val next = game.positions.indexOfFirst { it.ordinal > ordinal }
    val position = if (next >= 0) next else game.positions.lastIndex
    seek(position, ordinal)
  }

  fun seekToStop(ordinal: Int) {
    val game = shown ?: return
    game.positions.indexOfFirst { it.ordinal == ordinal }.takeIf { it >= 0 }?.let(::seek)
  }

  fun eventInstruction(ordinal: Int): String =
      eventInstructions[ordinal] ?: "Event $ordinal is not in this recording"

  fun stepStop(direction: Int): Boolean {
    focusedEvent = null
    return stepThrough(visibleStops.values.toList(), direction)
  }

  fun stepAction(direction: Int): Boolean {
    focusedEvent = null
    return stepThrough(visibleStops.values.filter { it in turnStops }, direction)
  }

  private fun stepThrough(stops: List<Int>, direction: Int): Boolean {
    if (shown == null || loading) return false
    val target =
        if (direction < 0) {
          stops.lastOrNull { it < eventOrdinal }
        } else {
          stops.firstOrNull { it > eventOrdinal }
        } ?: return false
    seekToStop(target)
    return true
  }

  fun seekToStart() {
    if (shown != null) seek(0)
  }

  private fun seek(position: Int, focus: Int? = null) {
    if (loading) return
    playbackMessage = null
    val game = shown ?: return
    game.seek(position)
    focusedEvent = focus
    navigationRevision++
    redraw()
  }

  /** Acts on [option] of the live game's prompt. */
  fun choose(option: Prompt.Option) = live { choose(option) }

  /** Lists more options of the live game's current decision. */
  fun showMore() = live(changed = false) { showMore() }

  /** Discards draft choices or cancels an open naming attempt. */
  fun retreat() = live { retreat() }

  /** Returns from history to the live game's latest position. */
  fun resumePlay() {
    val game = shown as? Live ?: return
    seek(game.positions.lastIndex)
  }

  /** Discards the live game's history after the viewed position and resumes play there. */
  fun truncate() = live { truncate() }

  /** Cancels provisional input first; otherwise discards the latest completed command. */
  fun undo(): Boolean {
    val game = shown as? Live ?: return false
    if (game.session.atTail && prompt?.retreat != null) {
      retreat()
      return true
    }
    val previous = game.positionIndex - 1
    if (previous < 0) return false
    seek(previous)
    truncate()
    return true
  }

  /** Runs [action] on the live game, then shows its effect; play happens only at the tail. */
  private fun live(changed: Boolean = true, action: PlaySession.() -> Unit) {
    val game = shown as? Live ?: return
    val failure = runCatching { game.session.action() }.exceptionOrNull()
    val displayFailure = runCatching {
      if (changed) open(gameName, game) else redraw()
    }
        .exceptionOrNull()
    error =
        when {
          displayFailure != null ->
              "Could not refresh the display: ${displayFailure.message}. The game remains at its latest completed command."
          failure != null -> failure.message ?: failure.toString()
          else -> null
        }
  }

  private companion object {
    val HAND = cn("Hand")
    val SELECTING = cn("Selecting")
  }
}
