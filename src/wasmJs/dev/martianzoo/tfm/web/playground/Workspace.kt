@file:OptIn(ExperimentalComposeUiApi::class)

package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Lays out every [Region] on a fixed-proportion canvas that scales uniformly to the available
 * width: one logical unit is one `dp` inside [content]. Narrowing the browser shrinks the whole
 * workspace; it never reflows it.
 *
 * Every region is also a mockup window. Near the region closest to the pointer a dashed outline
 * appears; dragging its edges or corners resizes it and dragging the strip just outside it moves
 * it, as does dragging anywhere inside it with Alt held. [onArrange] receives each intermediate
 * layout; [onArranged] is called once a drag ends.
 */
@Composable
internal fun Workspace(
    layout: MockupLayout,
    onArrange: (MockupLayout) -> Unit,
    onArranged: () -> Unit,
    content: @Composable (Region) -> Unit,
) {
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val scaled = Density(constraints.maxWidth / layout.canvas.width, fontScale = 1f)
    CompositionLocalProvider(LocalDensity provides scaled) {
      val pointer = remember { WindowPointer() }
      Box(
          Modifier.size(layout.canvas.width.dp, layout.canvas.height.dp)
              .mockupWindows(pointer, layout, onArrange, onArranged)
      ) {
        for (region in Region.entries) {
          val bounds = layout[region]
          Box(Modifier.offset(bounds.x.dp, bounds.y.dp).size(bounds.width.dp, bounds.height.dp)) {
            content(region)
          }
        }
        // Over a handle, a topmost layer shows its cursor; children's own cursors would win
        // otherwise.
        pointer.handle?.let { Box(Modifier.fillMaxSize().pointerHoverIcon(it.cursor)) }
      }
    }
  }
}

/** Which part of a window the pointer would drag. Edge handles combine for corners. */
private data class Handle(
    val region: Region,
    val left: Boolean,
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean,
) {
  val moves: Boolean
    get() = !left && !top && !right && !bottom

  val cursor: PointerIcon
    get() =
        when {
          moves -> MOVE_CURSOR
          (left && top) || (right && bottom) -> PointerIcon.fromKeyword("nwse-resize")
          (right && top) || (left && bottom) -> PointerIcon.fromKeyword("nesw-resize")
          left || right -> PointerIcon.fromKeyword("ew-resize")
          else -> PointerIcon.fromKeyword("ns-resize")
        }
}

private val MOVE_CURSOR = PointerIcon.fromKeyword("move")

/** How far outside a window its outline and move strip reach, in logical units. */
private const val REACH = 12f

/** Half-width of the band straddling each edge that resizes rather than moves. */
private const val EDGE = 5f

private const val MIN_SIZE = 24f

/**
 * The region nearest [point] within [REACH], and the handle there, if any. With [anywhere] (the Alt
 * key held), the inside of a region also moves it, since the strip between close neighbors is thin.
 */
private fun hitTest(
    layout: MockupLayout,
    point: Offset,
    anywhere: Boolean,
): Pair<Region, Handle?>? {
  val (region, bounds) =
      Region.entries
          .map { it to layout[it] }
          .filter { (_, b) -> distance(b, point) <= REACH }
          .minByOrNull { (_, b) -> distance(b, point) * 100_000 + b.width * b.height }
          ?: return null
  val nearX = point.y in bounds.y - EDGE..bounds.bottom + EDGE
  val nearY = point.x in bounds.x - EDGE..bounds.right + EDGE
  val left = nearX && abs(point.x - bounds.x) <= EDGE
  val right = nearX && abs(point.x - bounds.right) <= EDGE
  val top = nearY && abs(point.y - bounds.y) <= EDGE
  val bottom = nearY && abs(point.y - bounds.bottom) <= EDGE
  val handle =
      when {
        left || right || top || bottom -> Handle(region, left, top, right, bottom)
        anywhere || distance(bounds, point) > 0f -> Handle(region, false, false, false, false)
        else -> null
      }
  return region to handle
}

private fun distance(b: Bounds, p: Offset): Float {
  val dx = max(max(b.x - p.x, p.x - b.right), 0f)
  val dy = max(max(b.y - p.y, p.y - b.bottom), 0f)
  return max(dx, dy)
}

private fun Bounds.dragged(handle: Handle, dx: Float, dy: Float): Bounds {
  if (handle.moves) return copy(x = x + dx, y = y + dy)
  var l = x
  var t = y
  var r = right
  var b = bottom
  if (handle.left) l = minOf(l + dx, r - MIN_SIZE)
  if (handle.right) r = maxOf(r + dx, l + MIN_SIZE)
  if (handle.top) t = minOf(t + dy, b - MIN_SIZE)
  if (handle.bottom) b = maxOf(b + dy, t + MIN_SIZE)
  return Bounds(l, t, r - l, b - t)
}

private fun Bounds.rounded() =
    Bounds(
        x.roundToInt().toFloat(),
        y.roundToInt().toFloat(),
        width.roundToInt().toFloat(),
        height.roundToInt().toFloat(),
    )

/** What the pointer is over, or dragging: the region outlined and the handle under the pointer. */
private class WindowPointer {
  var outlined by mutableStateOf<Region?>(null)
  var handle by mutableStateOf<Handle?>(null)
}

@Composable
private fun Modifier.mockupWindows(
    pointer: WindowPointer,
    layout: MockupLayout,
    onArrange: (MockupLayout) -> Unit,
    onArranged: () -> Unit,
): Modifier {
  val currentLayout by rememberUpdatedState(layout)
  val arrange by rememberUpdatedState(onArrange)
  val arranged by rememberUpdatedState(onArranged)
  val outline = Color.White.copy(alpha = 0.5f)
  return this.pointerInput(Unit) {
        awaitPointerEventScope {
          var dragging: Handle? = null
          var start = Offset.Zero
          var startBounds: Bounds? = null
          while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.first()
            val point = change.position / density
            when (event.type) {
              PointerEventType.Press -> {
                val hit =
                    hitTest(currentLayout, point, event.keyboardModifiers.isAltPressed)?.second
                if (hit != null) {
                  dragging = hit
                  start = point
                  startBounds = currentLayout[hit.region]
                  change.consume()
                }
              }
              PointerEventType.Move,
              PointerEventType.Enter -> {
                val active = dragging
                val from = startBounds
                if (active != null && from != null) {
                  val moved = from.dragged(active, point.x - start.x, point.y - start.y).rounded()
                  arrange(currentLayout.with(active.region, moved))
                  change.consume()
                } else {
                  val hit = hitTest(currentLayout, point, event.keyboardModifiers.isAltPressed)
                  pointer.outlined = hit?.first
                  pointer.handle = hit?.second
                }
              }
              PointerEventType.Release -> {
                if (dragging != null) {
                  dragging = null
                  startBounds = null
                  change.consume()
                  arranged()
                }
              }
              PointerEventType.Exit -> {
                if (dragging == null) {
                  pointer.outlined = null
                  pointer.handle = null
                }
              }
            }
          }
        }
      }
      .drawWithContent {
        drawContent()
        val region = pointer.outlined ?: return@drawWithContent
        val b = currentLayout[region]
        val inset = 3.dp.toPx()
        drawRect(
            color = outline,
            topLeft = Offset(b.x.dp.toPx() - inset, b.y.dp.toPx() - inset),
            size = Size(b.width.dp.toPx() + 2 * inset, b.height.dp.toPx() + 2 * inset),
            style =
                Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                ),
        )
      }
}
