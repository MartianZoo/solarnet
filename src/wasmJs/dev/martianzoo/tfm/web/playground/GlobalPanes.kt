package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp

/** The tabs of the global workspace, in their natural order. */
internal enum class GlobalTab(val label: String) {
  Play("Play"),
  Players("Players"),
  Goals("Goals"),
  Colonies("Colonies"),
  Turmoil("Turmoil"),
  Effects("Effects"),
  Explorer("Explorer"),
  Config("Config"),
  PetsInfo("PETS info"),
}

private val TAB_HEIGHT = 32.dp

/**
 * A global pane with genuine tabs attached above it. The selected tab opens into the pane: the
 * pane's top border is interrupted where the tab joins it.
 */
@Composable
internal fun GlobalPane(
    tabs: List<GlobalTab>,
    selected: GlobalTab,
    onSelect: (GlobalTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalPalette.current
  var opening by remember { mutableStateOf(0f to 0f) }
  val tabScroll = rememberScrollState()
  Column(modifier.fillMaxSize()) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(tabScroll).height(TAB_HEIGHT),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Spacer(Modifier.width(palette.cornerRadius))
      for (tab in tabs) {
        Tab(
            tab,
            selected = tab == selected,
            onSelect = { onSelect(tab) },
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
              val (tabLeft, width) = opening
              val left = tabLeft - tabScroll.value
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
    onSelect: () -> Unit,
    onPlaced: (left: Float, width: Float) -> Unit,
) {
  val palette = LocalPalette.current
  val requester = remember { BringIntoViewRequester() }
  LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
  val r = palette.cornerRadius
  val shape = RoundedCornerShape(topStart = r, topEnd = r)
  Row(
      Modifier.fillMaxHeight(if (selected) 1f else 0.86f)
          .bringIntoViewRequester(requester)
          .onGloballyPositioned { onPlaced(it.positionInParent().x, it.size.width.toFloat()) }
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
          .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        tab.label,
        style = palette.body.copy(color = if (selected) palette.text else palette.dimText),
        maxLines = 1,
        softWrap = false,
    )
  }
}
