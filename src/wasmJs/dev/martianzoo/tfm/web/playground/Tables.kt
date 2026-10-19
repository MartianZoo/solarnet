package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val TASK_COLUMNS =
    listOf(
        TableColumn("#", Alignment.Center),
        TableColumn("WHO", Alignment.Center),
        TableColumn("INSTRUCTION"),
        TableColumn(""),
        TableColumn("GIVEN BY"),
        TableColumn("WHY", Alignment.Center),
    )

/**
 * TASK QUEUE (PENDING): every pending task in the world, with its raw Pets instruction. Clicking a
 * row selects it.
 */
@Composable
internal fun TaskQueue(
    useIcons: Boolean,
    widths: List<Float>,
    onResize: (List<Float>) -> Unit,
    onResized: () -> Unit,
    selected: Int?,
    causeInstruction: (Int) -> String,
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
      RawTable(TASK_COLUMNS, widths, onResize, onResized, rememberLazyListState(), gutter = 20.dp) {
        items(specimen.tasks, key = { it.id }) { task ->
          Row {
            Spacer(Modifier.width(20.dp))
            TableRow(
                widths,
                TASK_COLUMNS,
                height = 28.dp,
                highlighted = task.id == selected,
                onClick = { onSelect(task.id) },
                cells =
                    listOf(
                        { NumberText(task.id) },
                        { Tooltip("Controls the task") { ActorSquare(task.assignee) } },
                        {
                          TaskTooltip(task.fullInstruction, task.assignee) {
                            PetsText(task.fullInstruction, icons = useIcons, linkClasses = false)
                          }
                        },
                        {},
                        {
                          task.givenBy?.let {
                            PetsText(it, icons = useIcons, linkClasses = false)
                          }
                        },
                        {
                          CauseLink(task.because, causeInstruction, onShowCause)
                        },
                    ),
            )
          }
        }
      }
    }
  }
}

private val EVENT_COLUMNS =
    listOf(
        TableColumn("#", Alignment.Center),
        TableColumn("WHO", Alignment.Center),
        TableColumn("COMPONENT"),
        TableColumn("BAL", Alignment.CenterEnd),
        TableColumn("GIVEN BY"),
        TableColumn("WHY", Alignment.Center),
    )

/**
 * EVENT LOG (PAST): recorded changes in forward order. Ticks between rows on a left rail mark
 * player decision positions; event rows themselves do not move the timeline cursor.
 */
@Composable
internal fun EventLog(
    includeAdmin: Boolean,
    onIncludeAdmin: (Boolean) -> Unit,
    includeImaginary: Boolean,
    onIncludeImaginary: (Boolean) -> Unit,
    useIcons: Boolean,
    onUseIcons: (Boolean) -> Unit,
    useTrays: Boolean,
    onUseTrays: (Boolean) -> Unit,
    widths: List<Float>,
    onResize: (List<Float>) -> Unit,
    onResized: () -> Unit,
    checkpointOrdinal: Int,
    shown: List<Specimen.LoggedChange>,
    stops: Map<Int?, Int>,
    generationStarts: List<Int>,
    actionStops: List<RecordingStore.ActionStop>,
    referencedEvent: Int?,
    navigationRevision: Int,
    causeInstruction: (Int) -> String,
    onSeek: (Int) -> Unit,
    onSeekStop: (Int) -> Unit,
) {
  val palette = LocalPalette.current
  val navigationOrdinal = referencedEvent ?: (checkpointOrdinal - 1)
  val selectedGeneration = generationStarts.count { it <= navigationOrdinal }
  val actionScroll = rememberScrollState()
  val actionStep = with(LocalDensity.current) { 21.dp.roundToPx() }
  val actions = actionStops.filter { it.generation == selectedGeneration }
  val activeAction = actions.indexOfLast { it.eventOrdinal <= navigationOrdinal }
  LaunchedEffect(selectedGeneration, activeAction) {
    if (activeAction >= 0) {
      actionScroll.scrollTo(
          (activeAction * actionStep - actionScroll.viewportSize / 2).coerceAtLeast(0)
      )
    }
  }
  val listState = rememberLazyListState()
  val activeIndex = shown.indexOfFirst { it.ordinal >= checkpointOrdinal }
  val halfRow = with(LocalDensity.current) { 16.dp.roundToPx() }
  LaunchedEffect(navigationRevision, includeAdmin, includeImaginary, referencedEvent, shown.size) {
    val referenceIndex = referencedEvent?.let { target ->
      shown.indexOfFirst { it.ordinal == target }
    }
    listState.scrollToItem(
        referenceIndex?.takeIf { it >= 0 } ?: activeIndex.takeIf { it >= 0 } ?: shown.size,
        scrollOffset = if (referenceIndex != null && referenceIndex >= 0) 0 else -halfRow,
    )
  }
  Surface {
    Column(Modifier.fillMaxSize().padding(palette.spacing / 2)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TableTitle("EVENT LOG", "PAST", Modifier.weight(1f))
        Checkbox(
            includeAdmin,
            onIncludeAdmin,
            Modifier.width(26.dp).height(26.dp),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.dimText,
                ),
        )
        Text("Include Admin", style = palette.label)
        Spacer(Modifier.width(8.dp))
        Checkbox(
            includeImaginary,
            onIncludeImaginary,
            Modifier.width(26.dp).height(26.dp),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.dimText,
                ),
        )
        Text("Include imaginary", style = palette.label)
        Spacer(Modifier.width(8.dp))
        Checkbox(
            useIcons,
            onUseIcons,
            Modifier.width(26.dp).height(26.dp),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.dimText,
                ),
        )
        Text("Use icons", style = palette.label)
        Spacer(Modifier.width(8.dp))
        Checkbox(
            useTrays,
            onUseTrays,
            Modifier.width(26.dp).height(26.dp),
            colors =
                CheckboxDefaults.colors(
                    checkedColor = palette.accent,
                    uncheckedColor = palette.dimText,
                ),
        )
        Text("Use trays", style = palette.label)
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
            Tooltip(action.summary) {
              val color = actorColor(action.player)
              Canvas(
                  Modifier.width(18.dp).height(18.dp).clickable { onSeek(action.eventOrdinal) }
              ) {
                val triangle =
                    Path().apply {
                      moveTo(3.dp.toPx(), 2.dp.toPx())
                      lineTo(size.width - 2.dp.toPx(), size.height / 2)
                      lineTo(3.dp.toPx(), size.height - 2.dp.toPx())
                      close()
                    }
                drawPath(triangle, color)
                if (index == activeAction)
                    drawPath(triangle, palette.text, style = Stroke(1.5.dp.toPx()))
              }
            }
          }
        }
      }
      RawTable(
          EVENT_COLUMNS,
          widths,
          onResize,
          onResized,
          listState,
          modifier =
              Modifier.drawBehind {
                val x = 8.dp.toPx()
                drawLine(
                    palette.dimText.copy(alpha = 0.7f),
                    Offset(x, 23.dp.toPx()),
                    Offset(x, size.height),
                    1.dp.toPx(),
                )
              },
          centerRows = true,
          gutter = 20.dp,
      ) {
        items(shown, key = { it.ordinal }) { event ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(20.dp).height(32.dp)) {
              stops[event.ordinal]?.let { stop ->
                Tooltip("Position $stop") {
                  SafeStop(stop == checkpointOrdinal) { onSeekStop(stop) }
                }
              }
            }
            TableRow(
                widths,
                EVENT_COLUMNS,
                height = 32.dp,
                highlighted = event.ordinal == referencedEvent,
                dimmed = event.ordinal >= checkpointOrdinal,
                cellTooltip = { column -> event.notes.takeIf { column < EVENT_COLUMNS.lastIndex } },
                cells =
                    listOf(
                        { NumberText(event.ordinal) },
                        { ActorSquare(event.actor, 10.dp) },
                        {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterEnd) {
                              Delta(event)
                            }
                            ChangedComponent(event, useIcons)
                          }
                        },
                        { event.balance?.let { NumberText(it) } },
                        {
                          PetsText(
                              event.givenBy?.takeUnless { it.isBlank() } ?: "EX MACHINA",
                              icons = useIcons,
                          )
                        },
                        {
                          CauseLink(
                              event.because,
                              causeInstruction,
                              onSeek,
                          )
                        },
                    ),
            )
          }
        }
        item(key = "final-stops") {
          stops[null]?.let { stop ->
            Tooltip("Position $stop") {
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
      Modifier.width(20.dp).height(32.dp).clickable(onClick = onClick).padding(start = 8.dp),
      contentAlignment = Alignment.TopStart,
  ) {
    Box(
        Modifier.width(if (selected) 12.dp else 8.dp)
            .height(if (selected) 2.dp else 1.dp)
            .background(if (selected) palette.accent else palette.dimText.copy(alpha = 0.7f))
    )
  }
}

@Composable
private fun CauseLink(
    ordinal: Int?,
    instruction: (Int) -> String,
    onShow: (Int) -> Unit,
) {
  if (ordinal == null) return
  val palette = LocalPalette.current
  Tooltip(instruction(ordinal)) {
    Text(
        "$ordinal",
        Modifier.clickable { onShow(ordinal) },
        style = palette.mono,
        color = palette.text,
    )
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
private fun ChangedComponent(event: Specimen.LoggedChange, icons: Boolean) {
  Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    val gaining = event.gaining
    val removing = event.removing
    when {
      event.isSignal || removing == null -> PetsText(gaining ?: "", icons = icons)
      gaining == null -> PetsText(removing, icons = icons)
      else -> {
        PetsText(gaining, icons = icons)
        PlainPets("FROM")
        PetsText(removing, icons = icons)
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
