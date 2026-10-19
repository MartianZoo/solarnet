package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.GroundType

/** A row remembers only which part of its Pets type is open. */
@Stable
internal class ExplorerState {
  private sealed interface Expansion {
    data object Closed : Expansion

    data class Open(val path: List<Int>) : Expansion
  }

  private val expansions = mutableStateMapOf<List<GroundType>, Expansion>()
  var includeNonIconic by mutableStateOf(false)

  internal fun pivotFor(id: List<GroundType>): List<Int>? =
      when (val expansion = expansions[id]) {
        Expansion.Closed -> null
        is Expansion.Open -> expansion.path
        null -> if (id.size == 1) emptyList() else null
      }

  internal fun pivot(id: List<GroundType>, path: List<Int>) {
    expansions[id] = Expansion.Open(path)
  }

  internal fun toggle(id: List<GroundType>) {
    expansions[id] = if (pivotFor(id) == null) Expansion.Open(emptyList()) else Expansion.Closed
  }
}

/** The current game's components, grouped by the class or dependency clicked in each row. */
@Composable
internal fun ExplorerTab(
    state: ExplorerState,
    table: ClassTable,
) {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  val tree =
      remember(specimen, table, state.includeNonIconic) {
        ExplorerTree(table, specimen, state.includeNonIconic)
      }
  val lines = tree.lines(state::pivotFor)

  Column(Modifier.fillMaxSize().padding(palette.spacing)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(
          state.includeNonIconic,
          { state.includeNonIconic = it },
          Modifier.size(28.dp),
          colors =
              CheckboxDefaults.colors(
                  checkedColor = palette.accent,
                  uncheckedColor = palette.dimText,
              ),
      )
      Text("Include non-Iconic components", style = palette.body.copy(color = palette.dimText))
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
      items(lines) { line ->
        when (line) {
          is ExplorerTree.Line.Group ->
              GroupLine(
                  line,
                  onToggle = { state.toggle(line.id) },
                  onPivot = { state.pivot(line.id, it) },
              )
          is ExplorerTree.Line.Exact -> ExactLine(line)
        }
      }
    }
  }
}

@Composable
private fun GroupLine(
    line: ExplorerTree.Line.Group,
    onToggle: () -> Unit,
    onPivot: (List<Int>) -> Unit,
) {
  val palette = LocalPalette.current
  ExplorerRow(line.depth, line.count) {
    Text(
        if (line.pivot == null) "▸" else "▾",
        Modifier.width(14.dp).clickable(onClick = onToggle),
        style = palette.mono.copy(color = palette.dimText),
    )
    TypeLabel(line.type.expressionFull, emptyList(), line.pivot, onPivot)
  }
}

/** Every displayed argument comes from expressionFull, which omits class-fixed dependencies. */
@Composable
private fun TypeLabel(
    expression: Expression,
    path: List<Int>,
    pivot: List<Int>?,
    onPivot: (List<Int>) -> Unit,
) {
  val palette = LocalPalette.current
  val clicked =
      if (expression.className == CLASS && expression.arguments.size == 1) path + 0 else path
  Text(
      expression.className.toString(),
      Modifier.clickable { onPivot(clicked) },
      style = palette.mono.copy(color = if (clicked == pivot) palette.accent else palette.text),
  )
  if (expression.arguments.isNotEmpty()) {
    Text("<", style = palette.mono)
    expression.arguments.forEachIndexed { index, argument ->
      if (index > 0) Text(", ", style = palette.mono)
      TypeLabel(argument, path + index, pivot, onPivot)
    }
    Text(">", style = palette.mono)
  }
  expression.refinement?.let { Text("($it)", style = palette.mono) }
}

@Composable
private fun ExactLine(line: ExplorerTree.Line.Exact) {
  val palette = LocalPalette.current
  ExplorerRow(line.depth, line.entry.count) {
    Text(
        line.entry.type.expressionFull.toString(),
        Modifier.padding(start = 14.dp),
        style =
            palette.mono.copy(color = if (line.entry.nonIconic) palette.dimText else palette.text),
    )
  }
}

@Composable
private fun ExplorerRow(
    depth: Int,
    count: Int,
    label: @Composable () -> Unit,
) {
  val palette = LocalPalette.current
  Row(
      Modifier.fillMaxWidth()
          .height(22.dp)
          .background(palette.surface)
          .padding(start = (depth * 16).dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        "$count",
        Modifier.width(48.dp).padding(end = 6.dp),
        style = palette.mono.copy(color = palette.text, textAlign = TextAlign.End),
    )
    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { label() }
  }
}
