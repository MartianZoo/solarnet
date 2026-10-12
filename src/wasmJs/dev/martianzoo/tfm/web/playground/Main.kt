package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
public fun main() {
  ComposeViewport { MarsPlayground() }
}

/**
 * The mockup's layout and style, shared with the development server's layout file when there is
 * one. Writes happen only when an arrangement settles, never during a drag.
 */
@Stable
internal class LayoutStore {
  var layout by mutableStateOf(MockupLayout())
    private set

  /** Whether a development server is persisting the layout. */
  private var persisting = false
  private var lastWritten: String? = null

  fun connect() {
    loadLayoutText { text ->
      if (text != null) {
        persisting = true
        adopt(text)
        watchLayoutFile { loadLayoutText { changed -> changed?.let(::adopt) } }
      }
    }
  }

  private fun adopt(text: String) {
    val read = runCatching { MockupLayout.fromJson(text) }.getOrNull() ?: return
    lastWritten = read.toJson()
    layout = read
  }

  /** Shows [arranged] immediately without writing it. */
  fun arrange(arranged: MockupLayout) {
    layout = arranged
  }

  fun save() {
    if (!persisting) return
    val json = layout.toJson()
    if (json == lastWritten) return
    lastWritten = json
    saveLayoutText(json)
  }
}

@Composable
private fun MarsPlayground() {
  val recording = remember { RecordingStore() }
  val store = remember { LayoutStore().also { it.connect() } }
  LaunchedEffect(Unit) { recording.discover() }
  LaunchedEffect(Unit) {
    watchHistoryKeys { key ->
      when (key) {
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
    Column(
        Modifier.fillMaxSize().background(palette.background).verticalScroll(rememberScrollState())
    ) {
      val loaded = recording.specimen
      if (loaded != null) {
        CompositionLocalProvider(LocalSpecimen provides loaded) { Playground(store, recording) }
      } else {
        Text(recording.error ?: "Loading replay…", Modifier.padding(24.dp), style = palette.body)
      }
      Box(Modifier.fillMaxWidth().height(palette.borderWidth).background(palette.line))
      StyleControls(style) { store.arrange(store.layout.copy(style = it)) }
    }
  }
}

@Composable
private fun Playground(store: LayoutStore, recording: RecordingStore) {
  val specimen = LocalSpecimen.current
  val scope = rememberCoroutineScope()
  val tabs = remember { GlobalTabs(pinned = GlobalTab.Mars, shown = GlobalTab.Explorer) }
  var selectedTask by remember { mutableStateOf(specimen.tasks.find { it.selected }?.id) }
  LaunchedEffect(specimen.source.event) {
    selectedTask = specimen.tasks.find { it.selected }?.id
  }
  var selectedClass by remember { mutableStateOf<String?>(null) }
  val openClass: (String) -> Unit = { name ->
    selectedClass = name
    tabs.show(GlobalTab.PetsInfo)
  }
  var referencedEvent by remember(specimen.source.replay) { mutableStateOf<Int?>(null) }
  LaunchedEffect(recording.eventOrdinal) { referencedEvent = null }
  val explorer = remember { ExplorerState() }
  val viewed = remember(specimen.players.size) { ViewedPlayer(specimen.players.size) }
  val classNames =
      remember(specimen.source.replay) { specimen.classes.mapTo(hashSetOf()) { it.name } }
  val layout = store.layout

  @Composable
  fun TabContent(tab: GlobalTab) {
    when (tab) {
      GlobalTab.Mars -> MarsTab()
      GlobalTab.Players -> PlayersTab()
      GlobalTab.Goals -> GoalsTab()
      GlobalTab.Colonies -> ColoniesTab()
      GlobalTab.Turmoil -> TurmoilTab()
      GlobalTab.Explorer -> ExplorerTab(explorer, false, selectedClass)
      GlobalTab.Config -> ConfigTab(recording.config, recording.premise)
      GlobalTab.PetsInfo -> PetsInfoTab(selectedTask, selectedClass, recording.classTable)
    }
  }

  CompositionLocalProvider(LocalOpenClass provides openClass, LocalClassNames provides classNames) {
    Workspace(layout, store::arrange, store::save) { region ->
      when (region) {
        Region.Title -> TitleBar(Mode.HISTORY)
        Region.OutOfGame ->
            OutOfGame(recording.gameNames, recording::seekToStart) { name ->
              scope.launch { recording.load(name) }
            }
        Region.LeftPane ->
            GlobalPane(listOf(tabs.pinned), tabs.pinned, onSelect = {}, onPin = null) {
              TabContent(tabs.pinned)
            }
        Region.RightPane ->
            GlobalPane(tabs.unpinned, tabs.shown, tabs::show, tabs::pin) { TabContent(tabs.shown) }
        Region.GlobalDashboard -> GlobalDashboard()
        Region.MessageBar -> MessageBar(recording.gameName, recording.eventOrdinal, recording.error)
        Region.TaskQueue ->
            TaskQueue(
                layout.columns(MockupLayout.TASK_TABLE),
                { store.arrange(store.layout.withColumns(MockupLayout.TASK_TABLE, it)) },
                store::save,
                selectedTask,
                recording::isVisibleEvent,
                recording::hiddenEventDescription,
                { referencedEvent = it },
            ) {
              selectedTask = it
            }
        Region.EventLog ->
            EventLog(
                layout.columns(MockupLayout.EVENT_TABLE),
                { store.arrange(store.layout.withColumns(MockupLayout.EVENT_TABLE, it)) },
                store::save,
                recording.eventOrdinal,
                recording.stopOrdinals,
                recording.generationStarts,
                recording.actionStops,
                referencedEvent,
                recording::isVisibleEvent,
                recording::hiddenEventDescription,
                { referencedEvent = it },
                recording::seekToEvent,
                recording::seekToStop,
            )
        Region.PreviousPlayer -> PlayerArrow("<") { viewed.step(-1) }
        Region.PlayerDashboard -> PlayerSwipe(viewed) { PlayerDashboard(it) }
        Region.PlayedCards -> PlayerSwipe(viewed) { PlayedCards(it) }
        Region.NextPlayer -> PlayerArrow(">") { viewed.step(1) }
      }
    }
  }
}
