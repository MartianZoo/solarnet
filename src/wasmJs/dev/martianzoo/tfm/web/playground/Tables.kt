package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val TASK_COLUMNS =
    listOf(
        TableColumn("ID", end = true),
        TableColumn("WHO"),
        TableColumn("INSTRUCTION"),
        TableColumn("GIVEN BY"),
        TableColumn("BECAUSE", end = true),
    )

/** The squares in a task's WHO column, in order, with their exact tooltips. */
private val TASK_ROLES =
    listOf<Pair<String, (Specimen.PendingTask) -> String>>(
        "Decides when the task will happen" to { it.controller },
        "Makes choices for the task" to { it.selectionAssignee },
        "Actually carries out the task" to { it.assignee },
    )

/**
 * TASK QUEUE (PENDING): every pending task in the world, with its raw Pets instruction. Clicking a
 * row selects it.
 */
@Composable
internal fun TaskQueue(
    widths: List<Float>,
    onResize: (List<Float>) -> Unit,
    onResized: () -> Unit,
    selected: Int?,
    isVisibleCause: (Int) -> Boolean,
    hiddenCauseDescription: (Int) -> String,
    onShowCause: (Int) -> Unit,
    onSelect: (Int) -> Unit,
) {
  val specimen = LocalSpecimen.current
  Surface {
    Column(Modifier.fillMaxSize().padding(LocalPalette.current.spacing / 2)) {
      TableTitle("TASK QUEUE", "PENDING")
      if (specimen.tasks.isEmpty()) {
        Text("No tasks pending at this position.", style = LocalPalette.current.label)
      }
      RawTable(TASK_COLUMNS, widths, onResize, onResized, rememberLazyListState()) {
        items(specimen.tasks, key = { it.id }) { task ->
          TableRow(
              widths,
              TASK_COLUMNS,
              height = 28.dp,
              highlighted = task.id == selected,
              onClick = { onSelect(task.id) },
              cells =
                  listOf(
                      { NumberText(task.id) },
                      {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                          for ((tip, role) in TASK_ROLES) Tooltip(tip) { ActorSquare(role(task)) }
                        }
                      },
                      { PetsText(task.fullInstruction) },
                      { task.givenBy?.let { PetsText(it) } },
                      {
                        CauseLink(task.because, isVisibleCause, hiddenCauseDescription, onShowCause)
                      },
                  ),
          )
        }
      }
    }
  }
}

private val EVENT_COLUMNS =
    listOf(
        TableColumn("#", end = true),
        TableColumn("WHO"),
        TableColumn("", end = true),
        TableColumn("COMPONENT"),
        TableColumn("GIVEN BY"),
        TableColumn("BECAUSE", end = true),
    )

/**
 * EVENT LOG (PAST): recorded changes in forward order. Thin dividers mark player-request positions;
 * event rows themselves do not move the timeline cursor.
 */
@Composable
internal fun EventLog(
    widths: List<Float>,
    onResize: (List<Float>) -> Unit,
    onResized: () -> Unit,
    checkpointOrdinal: Int,
    stopOrdinals: List<Int>,
    generationStarts: List<Int>,
    actionStops: List<RecordingStore.ActionStop>,
    referencedEvent: Int?,
    isVisibleCause: (Int) -> Boolean,
    hiddenCauseDescription: (Int) -> String,
    onShowCause: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onSeekStop: (Int) -> Unit,
) {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  val selectedGeneration = generationStarts.count { it < checkpointOrdinal }
  val actionScroll = rememberScrollState()
  val actionStep = with(LocalDensity.current) { 19.dp.roundToPx() }
  val actions = actionStops.filter { it.generation == selectedGeneration }
  val activeAction = actions.indexOfLast { it.eventOrdinal < checkpointOrdinal }
  LaunchedEffect(selectedGeneration, activeAction) {
    if (activeAction >= 0) {
      actionScroll.scrollTo(
          (activeAction * actionStep - actionScroll.viewportSize / 2).coerceAtLeast(0)
      )
    }
  }
  var excludeAdmin by remember { mutableStateOf(false) }
  var excludeNonReal by remember { mutableStateOf(false) }
  val shown =
      specimen.events.filter { event ->
        (!excludeAdmin || event.actor != "Admin") && (!excludeNonReal || event.real)
      }
  val listState = rememberLazyListState()
  val activeIndex = shown.indexOfFirst { it.ordinal >= checkpointOrdinal }
  LaunchedEffect(referencedEvent) {
    if (referencedEvent != null) {
      excludeAdmin = false
      excludeNonReal = false
    }
  }
  LaunchedEffect(checkpointOrdinal, excludeAdmin, excludeNonReal, referencedEvent, shown.size) {
    val referenceIndex = referencedEvent?.let { target ->
      shown.indexOfFirst { it.ordinal == target }
    }
    listState.scrollToItem(
        referenceIndex?.takeIf { it >= 0 } ?: activeIndex.takeIf { it >= 0 } ?: shown.size
    )
  }
  Surface {
    Column(Modifier.fillMaxSize().padding(palette.spacing / 2)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TableTitle("EVENT LOG", "PAST", Modifier.weight(1f))
        Checkbox(
            excludeAdmin,
            { excludeAdmin = it },
            Modifier.width(26.dp).height(26.dp),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.dimText,
                ),
        )
        Text("Exclude Admin", style = palette.label)
        Spacer(Modifier.width(8.dp))
        Checkbox(
            excludeNonReal,
            { excludeNonReal = it },
            Modifier.width(26.dp).height(26.dp),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.dimText,
                ),
        )
        Text("Exclude non-Real", style = palette.label)
      }
      if (generationStarts.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          generationStarts.forEachIndexed { index, start ->
            val generation = index + 1
            Text(
                "Gen $generation",
                Modifier.background(
                        if (selectedGeneration == generation) palette.raised else palette.surface
                    )
                    .clickable {
                      onSeek(start)
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = if (selectedGeneration == generation) palette.body else palette.label,
            )
          }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(actionScroll).padding(bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          actions.forEachIndexed { index, action ->
            Tooltip("${action.player} action · event ${action.eventOrdinal}") {
              Box(
                  Modifier.width(16.dp)
                      .height(16.dp)
                      .border(1.dp, if (index == activeAction) palette.accent else palette.line)
                      .clickable { onSeek(action.eventOrdinal) },
                  contentAlignment = Alignment.Center,
              ) {
                ActorSquare(action.player, 10.dp)
              }
            }
          }
        }
      }
      RawTable(EVENT_COLUMNS, widths, onResize, onResized, listState) {
        itemsIndexed(shown, key = { _, event -> event.ordinal }) { index, event ->
          Column {
            val preceding = shown.getOrNull(index - 1)?.ordinal ?: -1
            stopOrdinals
                .filter { it > preceding && it <= event.ordinal }
                .forEach { stop ->
                  SafeStop(stop == checkpointOrdinal) { onSeekStop(stop) }
                }
            TableRow(
                widths,
                EVENT_COLUMNS,
                height = 20.dp,
                highlighted = event.ordinal == referencedEvent,
                dimmed = event.ordinal >= checkpointOrdinal,
                cells =
                    listOf(
                        { NumberText(event.ordinal) },
                        { ActorSquare(event.actor, 10.dp) },
                        { Delta(event) },
                        { ChangedComponent(event) },
                        { event.givenBy?.let { PetsText(it) } },
                        {
                          CauseLink(
                              event.because,
                              isVisibleCause,
                              hiddenCauseDescription,
                              onShowCause,
                          )
                        },
                    ),
            )
          }
        }
        item(key = "final-stops") {
          Column {
            val lastEvent = shown.lastOrNull()?.ordinal ?: -1
            stopOrdinals
                .filter { it > lastEvent }
                .forEach { stop ->
                  SafeStop(stop == checkpointOrdinal) { onSeekStop(stop) }
                }
          }
        }
      }
    }
  }
}

@Composable
private fun SafeStop(selected: Boolean, onClick: () -> Unit) {
  val palette = LocalPalette.current
  Box(
      Modifier.fillMaxWidth().height(4.dp).clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Box(
        Modifier.fillMaxWidth()
            .height(if (selected) 1.dp else 0.5.dp)
            .background(
                if (selected) palette.accent.copy(alpha = 0.45f)
                else palette.line.copy(alpha = 0.24f)
            )
    )
  }
}

@Composable
private fun CauseLink(
    ordinal: Int?,
    isVisible: (Int) -> Boolean,
    hiddenDescription: (Int) -> String,
    onShow: (Int) -> Unit,
) {
  if (ordinal == null) return
  val palette = LocalPalette.current
  if (isVisible(ordinal)) {
    Text(
        "$ordinal",
        Modifier.clickable { onShow(ordinal) },
        style = palette.mono,
        color = palette.accent,
    )
  } else {
    Tooltip(hiddenDescription(ordinal)) { NumberText(ordinal) }
  }
}

/** Gains and removals are signed; transmutations are unsigned; Signals show a star. */
@Composable
private fun Delta(event: Specimen.LoggedChange) {
  val palette = LocalPalette.current
  when {
    event.isSignal -> SignalStar(palette.mars, 9.dp)
    event.removing == null -> NumberText("+${event.count}", palette.text)
    event.gaining == null -> NumberText("−${event.count}", palette.text)
    else -> NumberText(event.count, palette.text)
  }
}

@Composable
private fun ChangedComponent(event: Specimen.LoggedChange) {
  // Room after the delta column is wider than before it.
  Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    val gaining = event.gaining
    val removing = event.removing
    when {
      event.isSignal || removing == null -> PetsText(gaining ?: "")
      gaining == null -> PetsText(removing)
      else -> {
        PetsText(removing)
        PlainPets("→")
        PetsText(gaining)
      }
    }
  }
}

/** A Signal change appears once, by its type, rather than as a transmutation into itself. */
private val Specimen.LoggedChange.isSignal: Boolean
  get() = signal && gaining == removing

@Composable
private fun TableTitle(name: String, tense: String, modifier: Modifier = Modifier) {
  val palette = LocalPalette.current
  Row(
      modifier.height(24.dp).padding(start = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(name, style = palette.label.copy(color = palette.text))
    Spacer(Modifier.width(6.dp))
    Text("($tense)", style = palette.label)
  }
}

/** A bare text field drawn as a line to write on. */
@Composable
internal fun Underlined(field: @Composable () -> Unit) {
  val palette = LocalPalette.current
  Column {
    field()
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
  }
}
