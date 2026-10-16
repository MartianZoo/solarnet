package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** What the live game waits on, its options as plain clickable rows, and the last refusal. */
@Composable
internal fun PlayTab(recording: RecordingStore, useIcons: Boolean) {
  val palette = LocalPalette.current
  val prompt = recording.prompt ?: return
  Column(
      Modifier.fillMaxSize().padding(palette.spacing * 1.6f),
      verticalArrangement = Arrangement.spacedBy(palette.spacing * 0.6f),
  ) {
    Text(
        prompt.heading,
        style = palette.body.copy(fontWeight = FontWeight.SemiBold, color = palette.accent),
    )
    prompt.detail?.let { ClassLinkedText(it) }
    if (recording.historyMode) {
      Text("Viewing history: use RESUME PLAY or TRUNCATE to play again.", style = palette.label)
    }
    recording.problem?.let { Text(it, style = palette.mono.copy(color = palette.mars)) }
    if (prompt.enabled && (prompt.retreat != null || prompt.more)) {
      Row(horizontalArrangement = Arrangement.spacedBy(palette.spacing)) {
        prompt.retreat?.let { OutlinedAction(it, palette.dimText, recording::retreat) }
        if (prompt.more) OutlinedAction("MORE", palette.dimText, recording::showMore)
      }
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth().alpha(if (prompt.enabled) 1f else 0.6f)) {
      items(prompt.options) { option ->
        val color =
            when {
              !option.enabled -> palette.dimText
              option.held -> palette.accent
              else -> palette.text
            }
        TaskTooltip(
            option.taskId?.let { option.label.substringAfter("  ") },
            option.who,
            Modifier.fillMaxWidth(),
        ) {
          Row(
              Modifier.fillMaxWidth()
                  .alpha(if (option.enabled) 1f else 0.45f)
                  .clip(RoundedCornerShape(palette.cornerRadius))
                  .then(
                      if (prompt.enabled && option.enabled)
                          Modifier.clickable { recording.choose(option) }
                      else Modifier
                  )
                  .then(if (option.held) Modifier.background(palette.raised) else Modifier)
                  .padding(horizontal = 6.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            if (option.who != null) {
              ActorSquare(option.who)
              Spacer(Modifier.width(8.dp))
            }
            if (useIcons) {
              // The row handles clicks, including those on text and inline icons.
              PetsText(
                  option.label,
                  modifier = Modifier.weight(1f),
                  color = color,
                  icons = true,
                  linkClasses = false,
              )
            } else
                Text(
                    option.label,
                    modifier = Modifier.weight(1f),
                    style = palette.mono,
                    color = color,
                )
          }
        }
      }
    }
  }
}
