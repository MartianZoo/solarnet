package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The product title and its mode badge: PLAY at a live game's latest position, HISTORY elsewhere.
 * Away from a live game's latest position, RESUME PLAY returns there and TRUNCATE discards the
 * later history so play continues from the viewed position.
 */
@Composable
internal fun TitleBar(recording: RecordingStore? = null) {
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
    val live = recording?.playing == true
    ModeBadge(if (live && !recording.historyMode) "PLAY" else "HISTORY")
    if (live && recording.historyMode) {
      Spacer(Modifier.width(palette.spacing * 2.4f))
      OutlinedAction("RESUME PLAY", palette.accent, recording::resumePlay)
      Spacer(Modifier.width(palette.spacing))
      OutlinedAction("TRUNCATE", palette.mars, recording::truncate)
    }
  }
}

@Composable
private fun ModeBadge(mode: String) {
  val palette = LocalPalette.current
  val shape = RoundedCornerShape((palette.cornerRadius - 2.dp).coerceAtLeast(0.dp))
  Box(
      Modifier.shadow(10.dp, shape, ambientColor = palette.accent, spotColor = palette.accent)
          .clip(shape)
          .background(palette.accent.copy(alpha = 0.22f))
          .border(palette.borderWidth, palette.accent, shape)
          .padding(horizontal = 12.dp, vertical = 5.dp)
  ) {
    Text(mode, style = palette.label.copy(color = palette.accent))
  }
}

/** Functions outside the game itself. */
@Composable
internal fun OutOfGame(
    recording: RecordingStore,
    onLoad: (String) -> Unit,
    onPlay: () -> Unit,
    onStartOver: () -> Unit,
) {
  val palette = LocalPalette.current
  Row(
      Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(palette.spacing, Alignment.End),
  ) {
    OutlinedAction("PLAY", palette.accent, onPlay)
    LoadGameAction(recording, onLoad)
    OutlinedAction("TO START", palette.mars, onStartOver)
  }
}

@Composable
internal fun LoadGameAction(recording: RecordingStore, onLoad: (String) -> Unit) {
  val palette = LocalPalette.current
  var choosing by remember { mutableStateOf(false) }
  OutlinedAction("LOAD GAME", palette.dimText) { choosing = true }
  if (choosing) {
    Dialog(
        onDismissRequest = { choosing = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
      BoxWithConstraints(
          Modifier.fillMaxSize().padding(48.dp),
          contentAlignment = Alignment.Center,
      ) {
        Surface(
            Modifier.width(maxWidth.coerceAtMost(1200.dp)).height(maxHeight.coerceAtMost(960.dp))
        ) {
          Column(
              Modifier.fillMaxSize().padding(24.dp),
              verticalArrangement = Arrangement.spacedBy(20.dp),
          ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text(
                  "Load a game",
                  Modifier.weight(1f),
                  style =
                      palette.body.copy(
                          fontSize = palette.body.fontSize * 1.6f,
                          fontWeight = FontWeight.SemiBold,
                      ),
              )
              OutlinedAction("CLOSE", palette.dimText) { choosing = false }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              if (recording.gameNames.isEmpty())
                  item { Text("No recordings found", style = palette.body) }
              items(recording.gameNames, key = { it }) { name ->
                GameChoice(name, recording) {
                  choosing = false
                  onLoad(name)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GameChoice(name: String, recording: RecordingStore, onLoad: () -> Unit) {
  val palette = LocalPalette.current
  val description = recording.describe(name)
  Column(
      Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(palette.cornerRadius))
          .background(palette.raised)
          .clickable(onClick = onLoad)
          .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(name, Modifier.weight(1f), style = palette.body.copy(fontWeight = FontWeight.SemiBold))
      description.second?.let { Text(it, style = palette.body) }
    }
    Text(description.first, style = palette.mono)
  }
}

@Composable
internal fun OutlinedAction(label: String, color: Color, onClick: () -> Unit) {
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
internal fun MessageBar(gameName: String, eventOrdinal: Int, error: String?, waitingFor: String?) {
  val palette = LocalPalette.current
  Surface(color = palette.raised) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = palette.spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
      Text(
          error
              ?: waitingFor
              ?: "$gameName · event $eventOrdinal · Up/down: decisions · Left/right: turns",
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
