package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Live look-and-feel controls beneath the workspace. Every change goes to the layout file. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StyleControls(style: Style, onStyle: (Style) -> Unit) {
  val palette = LocalPalette.current
  Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
    Text("LOOK AND FEEL", style = palette.label)
    Spacer(Modifier.height(12.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ColorField("background", style.background) { onStyle(style.copy(background = it)) }
      ColorField("surface", style.surface) { onStyle(style.copy(surface = it)) }
      ColorField("raised", style.raised) { onStyle(style.copy(raised = it)) }
      ColorField("line", style.line) { onStyle(style.copy(line = it)) }
      ColorField("accent", style.accent) { onStyle(style.copy(accent = it)) }
      ColorField("mars", style.mars) { onStyle(style.copy(mars = it)) }
      ColorField("text", style.text) { onStyle(style.copy(text = it)) }
      ColorField("dim text", style.dimText) { onStyle(style.copy(dimText = it)) }
      ColorField("admin", style.admin) { onStyle(style.copy(admin = it)) }
    }
    Spacer(Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Measure("border width", style.borderWidth, 0f..4f, 0.25f) {
        onStyle(style.copy(borderWidth = it))
      }
      Measure("corner radius", style.cornerRadius, 0f..20f, 1f) {
        onStyle(style.copy(cornerRadius = it))
      }
      Measure("shadow", style.shadow, 0f..24f, 1f) { onStyle(style.copy(shadow = it)) }
      Measure("spacing", style.spacing, 0f..24f, 1f) { onStyle(style.copy(spacing = it)) }
    }
    Spacer(Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      FontChoice("font", style.font, UiFont.entries) { onStyle(style.copy(font = it.name)) }
      Measure("font size", style.fontSize, 9f..22f, 0.5f) { onStyle(style.copy(fontSize = it)) }
      FontChoice("monospace font", style.monoFont, UiFont.entries.filter { it.monospace }) {
        onStyle(style.copy(monoFont = it.name))
      }
      Measure("monospace size", style.monoFontSize, 8f..20f, 0.5f) {
        onStyle(style.copy(monoFontSize = it))
      }
    }
  }
}

@Composable
private fun ColorField(label: String, value: String, onChange: (String) -> Unit) {
  val palette = LocalPalette.current
  var text by remember(value) { mutableStateOf(value) }
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
        Modifier.size(18.dp)
            .background(hexColor(value, Color.Transparent), RoundedCornerShape(3.dp))
            .border(1.dp, palette.line, RoundedCornerShape(3.dp))
    )
    Spacer(Modifier.width(6.dp))
    Column {
      Text(label, style = palette.label)
      Box(Modifier.width(76.dp)) {
        BasicTextField(
            text,
            {
              text = it
              if (hexColor(it, Color.Unspecified) != Color.Unspecified) onChange(it.trim())
            },
            textStyle = palette.mono.copy(color = palette.text),
            cursorBrush = SolidColor(palette.accent),
            singleLine = true,
            decorationBox = { Underlined(it) },
        )
      }
    }
  }
}

@Composable
private fun Measure(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onChange: (Float) -> Unit,
) {
  val palette = LocalPalette.current
  Column(Modifier.width(200.dp)) {
    Row {
      Text(label, Modifier.weight(1f), style = palette.label)
      Text(formatted(value), style = palette.mono.copy(color = palette.text))
    }
    Slider(
        value,
        { onChange((it / step).roundToInt() * step) },
        Modifier.height(26.dp),
        valueRange = range,
        colors =
            SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.line,
            ),
    )
  }
}

private fun formatted(value: Float): String {
  val hundredths = (value * 100).roundToInt()
  val whole = hundredths / 100
  val fraction = hundredths % 100
  return if (fraction == 0) "$whole"
  else "$whole.${fraction.toString().padStart(2, '0').trimEnd('0')}"
}

@Composable
private fun FontChoice(
    label: String,
    value: String,
    choices: List<UiFont>,
    onChange: (UiFont) -> Unit,
) {
  val palette = LocalPalette.current
  var open by remember { mutableStateOf(false) }
  val current = choices.find { it.name == value } ?: choices.first()
  Column(Modifier.width(200.dp)) {
    Text(label, style = palette.label)
    Box {
      Text(
          "${current.label}  ▾",
          Modifier.fillMaxWidth()
              .clickable { open = true }
              .border(palette.borderWidth, palette.line, RoundedCornerShape(4.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp),
          style = palette.body.copy(fontFamily = current.family()),
      )
      DropdownMenu(open, { open = false }, Modifier.background(palette.raised)) {
        for (choice in choices) {
          DropdownMenuItem({
            open = false
            onChange(choice)
          }) {
            Text(choice.label, style = palette.body.copy(fontFamily = choice.family()))
          }
        }
      }
    }
  }
}
