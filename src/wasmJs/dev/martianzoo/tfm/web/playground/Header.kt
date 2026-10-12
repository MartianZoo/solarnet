package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class Mode {
  SANDBOX,
  GAMEPLAY,
  HISTORY,
}

/** The product title with the mode group beside it. The selected mode glows. */
@Composable
internal fun TitleBar(mode: Mode) {
  val palette = LocalPalette.current
  Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
    Text(
        "Mars Playground",
        style =
            palette.body.copy(
                fontSize = palette.body.fontSize * 2f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = palette.body.fontSize * 0.02f,
            ),
        maxLines = 1,
        softWrap = false,
    )
    Spacer(Modifier.width(palette.spacing * 2.4f))
    Row(
        Modifier.clip(RoundedCornerShape(palette.cornerRadius))
            .border(palette.borderWidth, palette.line, RoundedCornerShape(palette.cornerRadius))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      for (m in Mode.entries) ModeButton(m.name, m == mode)
    }
  }
}

@Composable
private fun ModeButton(label: String, selected: Boolean) {
  val palette = LocalPalette.current
  val shape = RoundedCornerShape((palette.cornerRadius - 2.dp).coerceAtLeast(0.dp))
  Box(
      Modifier.let {
            if (selected)
                it.shadow(10.dp, shape, ambientColor = palette.accent, spotColor = palette.accent)
            else it
          }
          .clip(shape)
          .background(if (selected) palette.accent.copy(alpha = 0.22f) else Color.Transparent)
          .border(palette.borderWidth, if (selected) palette.accent else Color.Transparent, shape)
          .padding(horizontal = 12.dp, vertical = 5.dp)
  ) {
    Text(
        label,
        style = palette.label.copy(color = if (selected) palette.accent else palette.dimText),
    )
  }
}

/** Functions outside the game itself. */
@Composable
internal fun OutOfGame(
    games: List<String>,
    onStartOver: () -> Unit,
    onLoad: (String) -> Unit,
) {
  val palette = LocalPalette.current
  var choosing by remember { mutableStateOf(false) }
  Row(
      Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(palette.spacing, Alignment.End),
  ) {
    Box {
      OutlinedAction("LOAD SCENARIO", palette.dimText) { choosing = true }
      MaterialTheme(colors = darkColors(surface = palette.raised, onSurface = palette.text)) {
        DropdownMenu(choosing, onDismissRequest = { choosing = false }) {
          games.forEach { name ->
            DropdownMenuItem(
                onClick = {
                  choosing = false
                  onLoad(name)
                }
            ) {
              Text(name, style = palette.body)
            }
          }
        }
      }
    }
    OutlinedAction("START OVER", palette.mars, onStartOver)
  }
}

@Composable
private fun OutlinedAction(label: String, color: Color, onClick: () -> Unit) {
  val palette = LocalPalette.current
  val shape = RoundedCornerShape(palette.cornerRadius)
  Box(
      Modifier.clip(shape)
          .border(palette.borderWidth, color.copy(alpha = 0.8f), shape)
          .clickable(onClick = onClick)
          .padding(horizontal = 12.dp, vertical = 7.dp)
  ) {
    Text(label, style = palette.label.copy(color = color))
  }
}

/** Always says what the UI is waiting for the user to do next. */
@Composable
internal fun MessageBar(gameName: String, eventOrdinal: Int, error: String?) {
  val palette = LocalPalette.current
  Surface(color = palette.raised) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = palette.spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
      Text(
          error
              ?: "$gameName · event $eventOrdinal · Click a divider to seek; arrows step through history",
          style = palette.body.copy(color = palette.text),
      )
    }
  }
}

/** A thin vertical rule separating groups inside a bar. */
@Composable
internal fun Rule() {
  val palette = LocalPalette.current
  Box(
      Modifier.padding(horizontal = palette.spacing)
          .width(palette.borderWidth)
          .fillMaxHeight(0.6f)
          .background(palette.line)
  )
}
