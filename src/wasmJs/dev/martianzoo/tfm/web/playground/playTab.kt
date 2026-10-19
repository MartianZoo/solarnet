package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** What the live game waits on, its options as plain clickable text, and the last refusal. */
@Composable
internal fun PlayTab(recording: RecordingStore, useIcons: Boolean) {
  val palette = LocalPalette.current
  val prompt = recording.prompt ?: return
  Column(
      Modifier.fillMaxSize().padding(palette.spacing * 1.6f),
      verticalArrangement = Arrangement.spacedBy(palette.spacing * 0.6f),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
          prompt.heading,
          Modifier.weight(1f),
          style = palette.body.copy(fontWeight = FontWeight.SemiBold, color = palette.accent),
      )
      if (recording.playing) {
        Checkbox(
            recording.safeAutoexec,
            recording::setSafeAutoexec,
            enabled = !recording.historyMode,
            colors = CheckboxDefaults.colors(checkedColor = palette.accent),
        )
        Text(
            "Safe autoexec",
            Modifier.clickable(enabled = !recording.historyMode) {
              recording.setSafeAutoexec(!recording.safeAutoexec)
            },
            style = palette.label,
        )
      }
    }
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
        val instruction = option.taskId?.let { option.label.substringAfter("  ") }
        Row(
            Modifier.fillMaxWidth()
                .alpha(if (option.enabled) 1f else 0.45f)
                .then(if (option.held) Modifier.background(palette.raised) else Modifier)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          option.who?.let {
            ActorSquare(it)
            Spacer(Modifier.width(8.dp))
          }
          TaskTooltip(instruction, option.who) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              val parts =
                  if (instruction != null && option.arms.isNotEmpty())
                      inlineArms(instruction, option.arms)
                  else null
              if (parts == null) {
                TaskText(option.label, color, useIcons, prompt.enabled && option.enabled) {
                  recording.choose(option)
                }
              } else {
                Text(option.label.substringBefore("  "), style = palette.label)
                Spacer(Modifier.width(8.dp))
                parts.forEach { (text, arm) ->
                  if (arm == null)
                      PetsText(text, color = color, icons = useIcons, linkClasses = false)
                  else
                      TaskText(text, color, useIcons, prompt.enabled && option.enabled) {
                        recording.chooseTaskArm(option, arm)
                      }
                }
              }
            }
          }
        }
      }
    }
  }
}

/** Keeps the evaluated Pets instruction intact while making only each OR arm actionable. */
private fun inlineArms(instruction: String, arms: List<String>): List<Pair<String, String?>>? {
  var offset = 0
  return buildList {
    for (arm in arms) {
      val at = instruction.indexOf(arm, offset)
      if (at < 0) return null
      if (at > offset) add(instruction.substring(offset, at) to null)
      add(arm to arm)
      offset = at + arm.length
    }
    if (offset < instruction.length) add(instruction.substring(offset) to null)
  }
}

/** Text and inline icons glow on hover without drawing a row-sized shape. */
@Composable
private fun TaskText(
    text: String,
    color: Color,
    useIcons: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val palette = LocalPalette.current
  val source = remember { MutableInteractionSource() }
  val hovered by source.collectIsHoveredAsState()
  val active = enabled && hovered
  val modifier =
      if (enabled)
          Modifier.hoverable(source)
              .clickable(interactionSource = source, indication = null, onClick = onClick)
      else Modifier
  if (useIcons) {
    PetsText(
        text,
        modifier,
        if (active) palette.accent else color,
        icons = true,
        linkClasses = false,
        glow = active,
    )
  } else {
    Text(
        text,
        modifier,
        style =
            if (active) palette.mono.copy(shadow = Shadow(palette.accent, Offset.Zero, 12f))
            else palette.mono,
        color = if (active) palette.accent else color,
    )
  }
}
