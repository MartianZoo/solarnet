@file:OptIn(ExperimentalComposeUiApi::class)

package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A column heading and the alignment shared by its heading and cells. */
internal data class TableColumn(
    val heading: String,
    val alignment: Alignment = Alignment.CenterStart,
)

/**
 * A plain table of raw information in aligned columns. [widths] has one entry per column; the last
 * column takes whatever room remains. The thin dividers between columns drag horizontally,
 * reporting live widths to [onResize] and calling [onResized] when a drag ends.
 *
 * Scrolling stops at either end of the rows; the wheel never passes through to the page.
 */
@Composable
internal fun RawTable(
    columns: List<TableColumn>,
    widths: List<Float>,
    onResize: (List<Float>) -> Unit,
    onResized: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    centerRows: Boolean = false,
    gutter: Dp = 0.dp,
    rows: LazyListScope.() -> Unit,
) {
  val palette = LocalPalette.current
  Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
      Row(
          Modifier.fillMaxWidth().padding(start = gutter).height(22.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        columns.forEachIndexed { i, column ->
          Cell(widths, i, column.alignment) {
            Text(column.heading, style = palette.label, maxLines = 1, softWrap = false)
          }
        }
      }
      Box(Modifier.fillMaxWidth().height(palette.borderWidth).background(palette.line))
      BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().nestedScroll(StopAtEnds),
            state = listState,
            contentPadding =
                PaddingValues(
                    vertical = if (centerRows) (maxHeight / 2 - 16.dp).coerceAtLeast(0.dp) else 0.dp
                ),
            content = rows,
        )
      }
    }
    Box(Modifier.fillMaxSize().padding(start = gutter)) {
      ColumnDividers(widths, onResize, onResized)
    }
  }
}

/** One table row of [cells], aligned to [widths]. */
@Composable
internal fun TableRow(
    widths: List<Float>,
    columns: List<TableColumn>,
    height: Dp,
    highlighted: Boolean = false,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    cellTooltip: (Int) -> String? = { null },
    cells: List<@Composable () -> Unit>,
) {
  val palette = LocalPalette.current
  Row(
      Modifier.fillMaxWidth()
          .height(height)
          .alpha(if (dimmed) 0.72f else 1f)
          .background(if (highlighted) palette.accent.copy(alpha = 0.16f) else Color.Transparent)
          .let { if (onClick != null) it.clickable(onClick = onClick) else it },
      verticalAlignment = Alignment.CenterVertically,
  ) {
    cells.forEachIndexed { i, cell ->
      Cell(widths, i, columns[i].alignment, cellTooltip(i)) { cell() }
    }
  }
}

@Composable
private fun Cell(
    widths: List<Float>,
    index: Int,
    alignment: Alignment,
    tooltip: String? = null,
    content: @Composable () -> Unit,
) {
  val sized =
      if (index == widths.lastIndex) Modifier.fillMaxWidth() else Modifier.width(widths[index].dp)
  Tooltip(tooltip, sized.fillMaxHeight()) {
    Box(
        Modifier.fillMaxSize().clipToBounds().padding(horizontal = 5.dp),
        contentAlignment = alignment,
    ) {
      content()
    }
  }
}

/** A right-aligned number in the table's monospace face. */
@Composable
internal fun NumberText(value: Any?, color: Color? = null) {
  val palette = LocalPalette.current
  Text(
      value?.toString() ?: "",
      style = palette.mono,
      color = color ?: palette.dimText,
      textAlign = TextAlign.End,
      maxLines = 1,
      softWrap = false,
  )
}

@Composable
private fun ColumnDividers(
    widths: List<Float>,
    onResize: (List<Float>) -> Unit,
    onResized: () -> Unit,
) {
  val palette = LocalPalette.current
  val current by rememberUpdatedState(widths)
  val resize by rememberUpdatedState(onResize)
  val resized by rememberUpdatedState(onResized)
  val cursor = remember { PointerIcon.fromKeyword("col-resize") }
  var x = 0f
  for (i in 0 until widths.lastIndex) {
    x += widths[i]
    Box(
        Modifier.offset((x - 4f).dp)
            .width(8.dp)
            .fillMaxHeight()
            .pointerHoverIcon(cursor)
            .pointerInput(i) {
              val settle = {
                resize(current.map { it.roundToInt().toFloat() })
                resized()
              }
              detectHorizontalDragGestures(onDragEnd = settle, onDragCancel = settle) { change, dx
                ->
                change.consume()
                val next = current.toMutableList()
                next[i] = (next[i] + dx / density).coerceAtLeast(18f)
                resize(next)
              }
            },
        contentAlignment = Alignment.Center,
    ) {
      Box(Modifier.width(1.dp).fillMaxHeight().background(palette.line.copy(alpha = 0.55f)))
    }
  }
}

/** Consumes whatever scroll a list leaves over, so reaching its end never scrolls the page. */
private object StopAtEnds : NestedScrollConnection {
  override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
      available

  override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}
