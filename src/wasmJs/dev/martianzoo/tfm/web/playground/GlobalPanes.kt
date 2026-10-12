package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp

/** The tabs of the global workspace, in their natural order. */
internal enum class GlobalTab(val label: String) {
  Mars("Mars"),
  Players("Players"),
  Goals("Goals"),
  Colonies("Colonies"),
  Turmoil("Turmoil"),
  Explorer("Explorer"),
  Config("Config"),
  PetsInfo("PETS info"),
}

/**
 * Which tab is pinned to the left pane and which the right pane shows. The right pane never shows
 * the pinned tab.
 */
@Stable
internal class GlobalTabs(pinned: GlobalTab, shown: GlobalTab) {
  var pinned by mutableStateOf(pinned)
    private set

  var shown by mutableStateOf(shown)
    private set

  val unpinned: List<GlobalTab>
    get() = GlobalTab.entries - pinned

  /** Shows [tab] in the right pane, unless it is the pinned tab, which stays where it is. */
  fun show(tab: GlobalTab) {
    if (tab != pinned) shown = tab
  }

  /** Pins [tab] to the left pane; the former pinned tab returns to the right pane's strip. */
  fun pin(tab: GlobalTab) {
    val former = pinned
    if (tab == former) return
    pinned = tab
    if (shown == tab) shown = former
  }
}

private val TAB_HEIGHT = 32.dp

/**
 * A global pane with genuine tabs attached above it. The selected tab opens into the pane: the
 * pane's top border is interrupted where the tab joins it. [onPin] is null for the pinned pane.
 */
@Composable
internal fun GlobalPane(
    tabs: List<GlobalTab>,
    selected: GlobalTab,
    onSelect: (GlobalTab) -> Unit,
    onPin: ((GlobalTab) -> Unit)?,
    content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalPalette.current
  var opening by remember { mutableStateOf(0f to 0f) }
  Column(Modifier.fillMaxSize()) {
    Row(
        Modifier.fillMaxWidth().height(TAB_HEIGHT),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Spacer(Modifier.width(palette.cornerRadius))
      for (tab in tabs) {
        Tab(
            tab,
            selected = tab == selected,
            pinned = onPin == null,
            onSelect = { onSelect(tab) },
            onPin = onPin?.let { pin -> { pin(tab) } },
            onPlaced = { left, width -> if (tab == selected) opening = left to width },
        )
      }
    }
    val shape = RoundedCornerShape(palette.cornerRadius)
    Box(
        Modifier.fillMaxWidth()
            .weight(1f)
            .shadow(palette.shadow, shape, clip = false)
            .background(palette.surface, shape)
            .drawBehind {
              val w = palette.borderWidth.toPx()
              val r = palette.cornerRadius.toPx()
              drawRoundRect(
                  palette.line,
                  topLeft = Offset(w / 2, w / 2),
                  size = Size(size.width - w, size.height - w),
                  cornerRadius = CornerRadius(r, r),
                  style = Stroke(w),
              )
              val (left, width) = opening
              drawLine(
                  palette.surface,
                  Offset(left + w, w / 2),
                  Offset(left + width - w, w / 2),
                  strokeWidth = w * 2f,
              )
            }
            .clip(shape),
        content = content,
    )
  }
}

@Composable
private fun Tab(
    tab: GlobalTab,
    selected: Boolean,
    pinned: Boolean,
    onSelect: () -> Unit,
    onPin: (() -> Unit)?,
    onPlaced: (left: Float, width: Float) -> Unit,
) {
  val palette = LocalPalette.current
  val hover = remember { MutableInteractionSource() }
  val hovered by hover.collectIsHoveredAsState()
  val r = palette.cornerRadius
  val shape = RoundedCornerShape(topStart = r, topEnd = r)
  Row(
      Modifier.fillMaxHeight(if (selected) 1f else 0.86f)
          .onGloballyPositioned { onPlaced(it.positionInParent().x, it.size.width.toFloat()) }
          .hoverable(hover)
          .clip(shape)
          .background(if (selected) palette.surface else palette.raised.copy(alpha = 0.7f))
          .drawBehind {
            val w = palette.borderWidth.toPx()
            val radius = r.toPx()
            drawRoundRect(
                palette.line,
                topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height + radius),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(w),
            )
          }
          .clickable(onClick = onSelect)
          .padding(start = 14.dp, end = if (onPin == null && !pinned) 14.dp else 6.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        tab.label,
        style = palette.body.copy(color = if (selected) palette.text else palette.dimText),
        maxLines = 1,
        softWrap = false,
    )
    when {
      pinned -> Thumbtack(palette.accent, Modifier.padding(start = 6.dp))
      onPin != null ->
          Box(
              Modifier.size(16.dp).clickable(onClick = onPin),
              contentAlignment = Alignment.Center,
          ) {
            if (hovered) Thumbtack(palette.dimText)
          }
    }
    Spacer(Modifier.width(2.dp))
  }
}

/** A small thumbtack: a round head on a short needle. */
@Composable
private fun Thumbtack(color: Color, modifier: Modifier = Modifier) {
  Canvas(modifier.size(12.dp)) {
    val s = size.minDimension
    rotate(35f) {
      drawLine(
          color,
          Offset(s / 2, s * 0.45f),
          Offset(s / 2, s * 0.98f),
          s * 0.09f,
          StrokeCap.Round,
      )
      drawLine(
          color,
          Offset(s * 0.22f, s * 0.48f),
          Offset(s * 0.78f, s * 0.48f),
          s * 0.14f,
          StrokeCap.Round,
      )
      drawCircle(color, radius = s * 0.2f, center = Offset(s / 2, s * 0.26f))
    }
  }
}
