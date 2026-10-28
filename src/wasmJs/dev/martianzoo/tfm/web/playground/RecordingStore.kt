@file:OptIn(ExperimentalWasmJsInterop::class)

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
import dev.martianzoo.pets.ast.Instruction.Change
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
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

  fun reportError(message: String) {
    error = message
  }

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

  var completed by mutableStateOf(false)
    private set

  var liveLogConnected by mutableStateOf(false)
    private set

  var liveLogConnecting by mutableStateOf(false)
    private set

  var recoveryAvailable by mutableStateOf(false)
    private set

  private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var recoverySession: PlaySession? = null
  private var recoveryPending = false
  private var recoveryWriting = false
  private var recoveryVersion = 0
  private var logFolder: JsAny? = null
  private var logHandle: JsAny? = null
  private var logSession: PlaySession? = null
  private var logPending = false
  private var logForcePending = false
  private var logWriting = false
  private var logVersion = 0

  init {
    logScope.launch {
      val available = runCatching { savedRecovery() != null }.getOrDefault(false)
      if (recoverySession == null) recoveryAvailable = available
    }
    logScope.launch {
      try {
        val folder = savedLiveLogFolder() ?: return@launch
        if (liveLogConnecting) return@launch
        logFolder = folder
        liveLogConnected = hasLiveLogPermission(folder)
        if (liveLogConnected) (shown as? Live)?.session?.let(::startLiveLog)
      } catch (failure: Throwable) {
        error = "Could not restore live log folder: ${failure.message}"
      }
    }
  }

  var bootstrapEffects by mutableStateOf(emptySet<Effect>())
    private set

  private var shown: Shown? = null
  private var changes = emptyList<ChangeEvent>()
  private var balances = emptyMap<Int, Int>()
  private var measuredGame: Shown? = null
  private var balanceTracker: EventBalances? = null
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
  var safeAutoexec by mutableStateOf(true)
    private set

  private var turnStops = emptySet<Int>()
  private var undoStops = emptyList<Int>()

  val visibleEvents: List<Specimen.LoggedChange>
    get() =
        specimen?.events.orEmpty().filter { event ->
          event.ordinal == focusedEvent ||
              ((includeAdmin || event.actor != "Admin") && (includeImaginary || event.iconic))
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

  var previewing by mutableStateOf(false)
    private set

  private var skipRequested = false

  var playbackMessage by mutableStateOf<String?>(null)
    private set

  /** Opens saved events and previews the history; engine verification belongs to replay tests. */
  suspend fun load(name: String) {
    if (loading) return
    loading = true
    skipRequested = false
    playbackMessage = "Loading recording…"
    try {
      val (loadedConfig, active) = readGame(name)
      val recorded = Recorded(active)
      config = loadedConfig
      stopLiveLog()
      open(name.removeSuffix(".json"), recorded)
      recorded.seek(0)
      redraw()
      gameRevision++
      playbackMessage = "Playing recorded history…"
      previewing = true
      val last = recorded.positions.lastIndex
      val started = TimeSource.Monotonic.markNow()
      // Use elapsed time so expensive frames skip ahead instead of lengthening the preview.
      do {
        delay(50)
        val fraction = (started.elapsedNow().inWholeMilliseconds / 5000.0).coerceAtMost(1.0)
        recorded.seek(if (skipRequested) last else (last * fraction).toInt())
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
      previewing = false
      loading = false
    }
  }

  fun skipPreview() {
    if (previewing) skipRequested = true
  }

  /** Starts the browser's folder grant directly from a user click. */
  fun connectLiveLog(): Promise<JsAny>? {
    if (liveLogConnected || liveLogConnecting) return null
    liveLogConnecting = true
    return try {
      authorizeLiveLogFolder()
    } catch (failure: Throwable) {
      liveLogConnecting = false
      error = "Could not connect live log: ${failure.message}"
      null
    }
  }

  suspend fun attachLiveLog(selection: Promise<JsAny>): Boolean {
    try {
      val folder: JsAny = selection.await()
      if (!hasLiveLogPermission(folder)) {
        error = "Please allow this site to edit the chosen log folder."
        return false
      }
      // Verify that the promised file can actually be created before reporting a connection.
      openLiveLogFile(folder)
      logFolder = keepLiveLogFolder(folder)
      liveLogConnected = true
      (shown as? Live)?.session?.let { startLiveLog(it, writeInitial = true) }
      error = null
      return true
    } catch (failure: Throwable) {
      error = "Could not connect live log: ${failure.message}"
      return false
    } finally {
      liveLogConnecting = false
    }
  }

  /** Starts a live solo game; a failure leaves the shown game in place and reports why. */
  fun play(gameConfig: GameConfig, premise: GamePremise, seed: Int? = null): Boolean {
    if (loading) return false
    playbackMessage = null
    try {
      val session = PlaySession(premise, seed ?: Random.nextInt(), safeAutoexec)
      config = gameConfig
      stopLiveLog()
      open("Solo game", Live(session))
      queueRecovery(session)
      startLiveLog(session, writeInitial = true)
      error = null
      gameRevision++
      return true
    } catch (failure: Throwable) {
      error = "Could not start the game: ${failure.message}"
      return false
    }
  }

  /** Opens the last saved completed position with Player autoexec disabled. */
  suspend fun recoverLastGame(): Boolean {
    if (loading) return false
    loading = true
    try {
      val saved = requireNotNull(savedRecovery()) { "No saved live game was found" }
      val document = GameFileJson.parse(saved.gameFile)
      val session = recoverPlaySession(saved)
      safeAutoexec = false
      config = document.config
      stopLiveLog()
      open("Recovered solo game", Live(session))
      startLiveLog(session, writeInitial = true)
      gameRevision++
      error = null
      return true
    } catch (failure: Throwable) {
      error = "Could not recover the last game: ${failure.message}"
      return false
    } finally {
      loading = false
    }
  }

  /** Keeps the previous completed position available even when the next browser click stalls. */
  private fun queueRecovery(session: PlaySession) {
    if (recoverySession !== session) {
      recoverySession = session
      recoveryVersion++
    }
    recoveryPending = true
    if (recoveryWriting) return
    recoveryWriting = true
    val version = recoveryVersion
    logScope.launch {
      try {
        do {
          recoveryPending = false
          val snapshot = session.recovery()
          if (version != recoveryVersion) break
          saveRecovery(snapshot)
          if (version == recoveryVersion) recoveryAvailable = true
        } while (recoveryPending && version == recoveryVersion)
      } catch (failure: Throwable) {
        if (version == recoveryVersion) {
          error = "Could not save game recovery: ${failure.message}"
        }
      } finally {
        recoveryWriting = false
        if (recoveryPending) recoverySession?.let(::queueRecovery)
      }
    }
  }

  private fun startLiveLog(session: PlaySession, writeInitial: Boolean = false) {
    logSession = session
    val folder = logFolder ?: return
    if (!liveLogConnected) return
    val version = logVersion
    logScope.launch {
      try {
        val handle = openLiveLogFile(folder)
        if (version != logVersion || (shown as? Live)?.session !== session) return@launch
        logHandle = handle
        if (writeInitial) queueLiveLog(force = true) else if (logPending) queueLiveLog()
      } catch (failure: Throwable) {
        if (version == logVersion) error = "Could not save live log: ${failure.message}"
      }
    }
  }

  fun stopLiveLog() {
    logVersion++
    logHandle = null
    logSession = null
    logPending = false
    logForcePending = false
  }

  private fun queueLiveLog(force: Boolean = false) {
    logPending = true
    logForcePending = logForcePending || force
    if (logHandle == null || logWriting) return
    logWriting = true
    val version = logVersion
    logScope.launch {
      try {
        while (logPending) {
          logPending = false
          logForcePending = false
          val handle = logHandle ?: break
          val session = logSession ?: break
          if ((shown as? Live)?.session !== session || !session.atTail) continue
          val contents = session.replayFile()
          if (handle !== logHandle) continue
          overwriteLiveLogFile(handle, contents)
        }
      } catch (failure: Throwable) {
        if (version == logVersion) {
          stopLiveLog()
          liveLogConnected = false
          error = "Could not save live log: ${failure.message}"
        }
      } finally {
        logWriting = false
        if (logPending && logHandle != null) queueLiveLog()
      }
    }
  }

  private fun open(name: String, game: Shown) {
    val entries = game.events.entriesSince(Checkpoint(0))
    val tracker =
        balanceTracker?.takeIf { measuredGame === game && it.canExtend(entries) }
            ?: EventBalances(game.premise).also {
              measuredGame = game
              balanceTracker = it
            }
    val measuredBalances = tracker.extend(entries)
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
                (cause?.change?.gaining?.className in setOf(cn("Baton"), cn("OfferAction")) ||
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
    val discardTasks = mutableListOf<Pair<Task, Int>>()
    var phase: String? = null
    for (event in entries) {
      when (event) {
        is ChangeEvent -> {
          val gained = event.change.gaining?.className?.toString()
          if (gained in setOf("SetupPhase", "CorporationPhase", "ResearchPhase", "ActionPhase"))
              phase = gained
        }
        is TaskAddedEvent -> {
          val change = event.task.instruction as? Change
          val source = change?.removing
          val card = source?.let { game.reader.resolve(it).rootClass.className }
          if (
              phase in setOf("SetupPhase", "CorporationPhase", "ResearchPhase") &&
                  event.task.assignee is Player &&
                  change?.gaining == null &&
                  source != null &&
                  ((card == cn("CorporationCard") && phase != "ResearchPhase") ||
                      (card == cn("ProjectCard") && "Selecting" in source.toString()))
          )
              discardTasks += event.task to event.ordinal
        }
        else -> Unit
      }
    }
    val addedTasks = entries.filterIsInstance<TaskAddedEvent>()
    val removedAt =
        entries.filterIsInstance<TaskRemovedEvent>().associate { it.task.id to it.ordinal }
    val discardStops = discardTasks.mapNotNull { (task, added) ->
      val preceding = addedTasks.filter { event ->
        event.task.assignee == task.assignee &&
            event.task.cause == task.cause &&
            event.task.id.ordinal < task.id.ordinal
      }
      val completions = preceding.map { prior ->
        removedAt[prior.task.id] ?: return@mapNotNull null
      }
      val ready = maxOf(added, completions.maxOrNull() ?: added)
      game.positions
          .firstOrNull { it.ordinal > ready }
          ?.ordinal
          ?.takeIf { stop ->
            stop >= (live?.start?.ordinal ?: 0) &&
                entries.none {
                  it is TaskRemovedEvent &&
                      it.task.id == task.id &&
                      it.ordinal < stop &&
                      it.ordinal > added
                }
          }
    }
    undoStops = (turns + discardStops + listOfNotNull(live?.start?.ordinal)).distinct().sorted()
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
    completed = live?.completed == true
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

  fun chooseTaskArm(option: Prompt.Option, arm: String) = live { chooseTaskArm(option, arm) }

  fun cardActionAvailable(card: String, slot: Int): Boolean =
      (shown as? Live)?.session?.takeIf { !historyMode }?.cardActionAvailable(card, slot) == true

  fun projectCardAvailable(card: String): Boolean =
      (shown as? Live)?.session?.takeIf { !historyMode }?.projectCardAvailable(card) == true

  fun playProjectCard(card: String) = live { playProjectCard(card) }

  fun toggleCard(name: String) = live(changed = false) { toggleCard(name) }

  fun confirmCards() = live { confirmCards() }

  fun setSafeAutoexec(enabled: Boolean) {
    if (historyMode || safeAutoexec == enabled) return
    safeAutoexec = enabled
    live { setSafeAutoexec(enabled) }
  }

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
  fun truncate() {
    balanceTracker = null
    live { truncate() }
  }

  /** Returns to the beginning of the current action, or the preceding action at its start. */
  fun undo(): Boolean {
    val game = shown as? Live ?: return false
    if (!game.session.atTail) return false
    val before = game.session.world.timeline.checkpoint()
    val draft = game.session.hasDraftInput
    val candidates =
        undoStops.asReversed().filter { it < before.ordinal || (draft && it == before.ordinal) }
    var restored = false
    for (ordinal in candidates) {
      val checkpoint = game.session.stops.firstOrNull { it.ordinal == ordinal } ?: continue
      restored =
          runCatching {
            game.session.seek(checkpoint)
            game.session.truncate()
            game.session.prompt()
          }
              .isSuccess
      if (restored) break
    }
    if (!restored) return false
    safeAutoexec =
        game.session.agents[game.session.player].autoExecPolicy ==
            dev.martianzoo.agent.AutoExecPolicy.SAFE
    balanceTracker = null
    open(gameName, game)
    if (game.session.world.timeline.checkpoint() != before) {
      queueRecovery(game.session)
      queueLiveLog(force = true)
    }
    error = null
    return true
  }

  /** Runs [action] on the live game, then shows its effect; play happens only at the tail. */
  private fun live(
      changed: Boolean = true,
      action: PlaySession.() -> Unit,
  ) {
    val game = shown as? Live ?: return
    val before = game.session.world.timeline.positions.last()
    val failure = runCatching { game.session.action() }.exceptionOrNull()
    val after = game.session.world.timeline.positions.last()
    if (after != before && game.session.atTail) queueRecovery(game.session)
    val displayFailure = runCatching {
      if (changed && after != before) open(gameName, game) else redraw()
    }
        .exceptionOrNull()
    error =
        when {
          displayFailure != null ->
              "Could not refresh the display: ${displayFailure.message}. The game remains at its latest completed command."
          failure != null -> failure.message ?: failure.toString()
          else -> null
        }
    // Every committed position is logged, so the file shows the state before any later failure.
    if (displayFailure == null && after != before) queueLiveLog(force = true)
  }

  private companion object {
    val HAND = cn("Hand")
    val SELECTING = cn("Selecting")
  }
}
