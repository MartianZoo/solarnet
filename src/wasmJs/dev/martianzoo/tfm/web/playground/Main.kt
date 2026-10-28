@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import dev.martianzoo.tfm.text.EnglishInstructionTextRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.configureWebResources

@OptIn(ExperimentalComposeUiApi::class)
public fun main() {
  configureWebResources {
    resourcePathMapping { path ->
      val imagePath = path.substringAfter("/files/images/", "")
      if (imagePath.isEmpty()) "./$path" else "https://www.martianzoo.dev/images/$imagePath"
    }
  }
  ComposeViewport { MarsPlayground() }
}

/**
 * The mockup's in-memory layout and style, initialized from code. Adjustments are written for
 * review only when an arrangement settles, never read back from the snapshot.
 */
@Stable
internal class LayoutStore {
  var layout by mutableStateOf(MockupLayout())
    private set

  private var lastWritten: String = layout.toJson()

  /** Shows [arranged] immediately without writing it. */
  fun arrange(arranged: MockupLayout) {
    layout = arranged
  }

  fun save() {
    val json = layout.toJson()
    if (json == lastWritten) return
    lastWritten = json
    saveLayoutText(json)
  }
}

@Composable
private fun MarsPlayground() {
  val recording = remember { RecordingStore() }
  val store = remember { LayoutStore() }
  val scope = rememberCoroutineScope()
  val loadGame: (String) -> Unit = { name -> scope.launch { recording.load(name) } }
  val recoverGame: () -> Unit = { scope.launch { recording.recoverLastGame() } }
  val newGame = remember { NewGameDraft() }
  var savedSetups by remember {
    mutableStateOf(runCatching { savedGameSetups() }.getOrDefault(emptyList()))
  }
  var choosingGame by remember { mutableStateOf(false) }
  var startupError by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) { recording.discover() }
  LaunchedEffect(Unit) {
    watchHistoryKeys { key ->
      if (recording.loading) return@watchHistoryKeys true
      if (choosingGame) return@watchHistoryKeys false
      when (key) {
        "Undo" -> recording.undo()
        "ArrowUp" -> recording.stepStop(-1)
        "ArrowDown" -> recording.stepStop(1)
        "ArrowLeft" -> recording.stepAction(-1)
        "ArrowRight" -> recording.stepAction(1)
        else -> false
      }
    }
  }
  val style = store.layout.style
  LaunchedEffect(style) {
    delay(400)
    store.save()
  }
  PlaygroundTheme(style) {
    val palette = LocalPalette.current
    Box(Modifier.fillMaxSize().onPreviewKeyEvent { recording.loading }) {
      Column(
          Modifier.fillMaxSize()
              .background(palette.background)
              .verticalScroll(rememberScrollState())
      ) {
        val loaded = recording.specimen
        if (loaded != null) {
          CompositionLocalProvider(LocalSpecimen provides loaded) {
            Playground(store, recording, loadGame, recoverGame) { choosingGame = true }
          }
          Box(Modifier.fillMaxWidth().height(palette.borderWidth).background(palette.line))
          StyleControls(style) { store.arrange(store.layout.copy(style = it)) }
        } else {
          Row(
              Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 24.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(Modifier.weight(1f).fillMaxHeight()) { TitleBar() }
            OutlinedAction("PLAY", palette.accent) { choosingGame = true }
            Spacer(Modifier.width(palette.spacing))
            if (recording.recoveryAvailable) {
              OutlinedAction("RECOVER GAME", palette.accent, recoverGame)
              Spacer(Modifier.width(palette.spacing))
            }
            LoadGameAction(recording, loadGame)
          }
          Text(
              recording.error
                  ?: recording.playbackMessage
                  ?: "Choose PLAY for a solo game, or LOAD GAME to browse a recording.",
              Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
              style = palette.body,
          )
        }
      }
      if (recording.loading) {
        Box(
            Modifier.matchParentSize().pointerInput(Unit) {
              awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
              }
            }
        )
      }
      if (recording.previewing) {
        Box(
            Modifier.matchParentSize().padding(24.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
          OutlinedAction("SKIP PREVIEW", palette.accent, recording::skipPreview)
        }
      }
    }
    if (choosingGame) {
      NewGameDialog(
          newGame,
          savedSetups,
          { choosingGame = false },
          startupError,
          !recording.liveLogConnected,
          recording.liveLogConnecting,
      ) { config, premise, seed ->
        val needsFolder = !recording.liveLogConnected
        if (needsFolder) choosingGame = false
        val logSelection =
            if (needsFolder && !recording.liveLogConnecting) recording.connectLiveLog() else null
        val startGame = {
          if (recording.play(config, premise, seed)) {
            choosingGame = false
            startupError = null
            runCatching {
              savedSetups =
                  rememberGameSetup(
                      SavedGameSetup(
                          newGame.entries.toList(),
                          newGame.playerName,
                          requireNotNull(recording.seed),
                      )
                  )
            }
                .onFailure {
                  recording.reportError(
                      "Game started, but its setup could not be saved: ${it.message}"
                  )
                }
          } else {
            startupError = recording.error
            choosingGame = true
          }
        }
        startGame()
        if (logSelection != null) {
          scope.launch { recording.attachLiveLog(logSelection) }
        }
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Playground(
    store: LayoutStore,
    recording: RecordingStore,
    onLoad: (String) -> Unit,
    onRecover: () -> Unit,
    onPlay: () -> Unit,
) {
  val specimen = LocalSpecimen.current
  var shownTab by remember { mutableStateOf(GlobalTab.Explorer) }
  var useIcons by remember { mutableStateOf(true) }
  var useTrays by remember { mutableStateOf(true) }
  var publishedCardText by remember { mutableStateOf(false) }
  val handOrders = remember(recording.gameRevision) { mutableStateMapOf<String, List<String>>() }
  val tabs = availableTabs(specimen, recording.playing)
  val selectedTab = shownTab.takeIf { it in tabs } ?: GlobalTab.Explorer
  LaunchedEffect(recording.gameRevision) {
    shownTab = if (recording.playing) GlobalTab.Play else GlobalTab.Explorer
  }
  var selectedTask by remember { mutableStateOf(specimen.tasks.find { it.selected }?.id) }
  LaunchedEffect(specimen.source.event) {
    selectedTask = specimen.tasks.find { it.selected }?.id
  }
  val explorer = remember(recording.classTable) { ExplorerState() }
  val viewed = remember(specimen.players.size) { ViewedPlayer(specimen.players.size) }
  val classNames =
      remember(recording.classTable) {
        requireNotNull(recording.classTable).allClasses().mapTo(hashSetOf()) {
          it.className.toString()
        }
      }
  val layout = store.layout

  @Composable
  fun TabContent(tab: GlobalTab) {
    when (tab) {
      GlobalTab.Play -> PlayTab(recording, useIcons)
      GlobalTab.Players -> PlayersTab()
      GlobalTab.Goals -> GoalsTab()
      GlobalTab.Colonies -> ColoniesTab()
      GlobalTab.Turmoil -> TurmoilTab()
      GlobalTab.Effects ->
          EffectsTab(
              requireNotNull(recording.classTable),
              recording.bootstrapEffects,
              recording.includeImaginary,
              useIcons,
          )
      GlobalTab.Explorer -> ExplorerTab(explorer, requireNotNull(recording.classTable))
      GlobalTab.Config -> ConfigTab(recording.config, recording.premise, recording.seed)
    }
  }

  CompositionLocalProvider(
      LocalOpenClass provides ::openAlmanacClass,
      LocalClassNames provides classNames,
      LocalPlayChoices provides
          recording.prompt?.let {
            PlayChoices(
                it,
                recording::choose,
                recording::chooseTaskArm,
                { recording.prompt },
                recording::cardActionAvailable,
                recording::projectCardAvailable,
                recording::playProjectCard,
            )
          },
      LocalUseTrays provides useTrays,
      LocalInstructionEnglish provides
          remember(recording.classTable) {
            recording.classTable?.let { EnglishInstructionTextRenderer(it) }
          },
  ) {
    Workspace(layout, store::arrange, store::save) { region ->
      when (region) {
        Region.Title -> TitleBar(recording)
        Region.OutOfGame -> OutOfGame(recording, onLoad, onPlay, onRecover, recording::seekToStart)
        Region.Mars -> Surface { MarsTab() }
        Region.Details ->
            GlobalPane(
                tabs,
                selectedTab,
                { shownTab = it },
            ) {
              TabContent(selectedTab)
            }
        Region.GlobalDashboard -> GlobalDashboard()
        Region.MessageBar ->
            MessageBar(
                recording.gameName,
                recording.eventOrdinal,
                recording.error ?: recording.problem,
                recording.playbackMessage ?: recording.prompt?.heading,
            )
        Region.History ->
            Column(Modifier.fillMaxSize()) {
              val fraction = layout.eventLogFraction.coerceIn(0.2f, 0.85f)
              val density = LocalDensity.current.density
              val palette = LocalPalette.current
              Box(Modifier.weight(fraction).fillMaxWidth()) {
                EventLog(
                    recording.includeAdmin,
                    { recording.includeAdmin = it },
                    recording.includeImaginary,
                    { recording.includeImaginary = it },
                    useIcons,
                    { useIcons = it },
                    useTrays,
                    { useTrays = it },
                    layout.columns,
                    { store.arrange(store.layout.copy(columns = it)) },
                    store::save,
                    recording.eventOrdinal,
                    recording.visibleEvents,
                    recording.visibleStops,
                    recording.generationStarts,
                    recording.actionStops,
                    recording.focusedEvent,
                    recording.navigationRevision,
                    recording::eventInstruction,
                    recording::seekToEvent,
                    recording::seekToStop,
                )
              }
              Box(
                  Modifier.fillMaxWidth()
                      .height(16.dp)
                      .pointerHoverIcon(PointerIcon.fromKeyword("row-resize"))
                      .draggable(
                          state =
                              rememberDraggableState { delta ->
                                val current = store.layout
                                val height =
                                    (current[Region.History].height - 16f).coerceAtLeast(1f)
                                store.arrange(
                                    current.copy(
                                        eventLogFraction =
                                            (current.eventLogFraction + delta / density / height)
                                                .coerceIn(0.2f, 0.85f)
                                    )
                                )
                              },
                          orientation = Orientation.Vertical,
                          onDragStopped = { store.save() },
                      ),
                  contentAlignment = Alignment.Center,
              ) {
                Box(Modifier.fillMaxWidth().height(palette.borderWidth).background(palette.line))
                Box(Modifier.width(40.dp).height(3.dp).background(palette.accent))
              }
              Box(Modifier.weight(1f - fraction).fillMaxWidth()) {
                TaskQueue(
                    useIcons,
                    layout.columns,
                    { store.arrange(store.layout.copy(columns = it)) },
                    store::save,
                    selectedTask,
                    recording::eventInstruction,
                    recording::seekToEvent,
                ) {
                  selectedTask = it
                }
              }
            }
        Region.PreviousPlayer -> PlayerArrow("<") { viewed.step(-1) }
        Region.PlayerDashboard -> PlayerSwipe(viewed) { PlayerDashboard(it) }
        Region.PlayedCards ->
            PlayerSwipe(viewed) { player ->
              PlayedCards(
                  player,
                  publishedCardText,
                  { publishedCardText = it },
                  handOrders[player.name].orEmpty(),
                  { handOrders[player.name] = it },
                  recording.prompt?.cards?.takeIf { recording.playing && !recording.historyMode },
                  recording::toggleCard,
              )
            }
        Region.NextPlayer -> PlayerArrow(">") { viewed.step(1) }
      }
    }
  }
}
