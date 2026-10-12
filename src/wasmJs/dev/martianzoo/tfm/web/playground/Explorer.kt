package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Class relationships among the types present in the specimen. */
internal class TypeTree(specimen: Specimen) {
  private val classes = specimen.classes.associateBy { it.name }
  private val ancestorCache = mutableMapOf<String, Set<String>>()

  fun info(name: String): Specimen.ClassInfo? = classes[name]

  /** [name] and every class it extends. */
  fun ancestors(name: String): Set<String> =
      ancestorCache.getOrPut(name) {
        setOf(name) + info(name)?.superclasses.orEmpty().flatMap(::ancestors)
      }

  /**
   * The path from the root class down to [name], following each class's first-declared superclass.
   * The Explorer files a type only along this path, so subdividing a node never counts a component
   * twice.
   */
  fun primaryPath(name: String): List<String> {
    val path = mutableListOf(name)
    while (true) path += info(path.last())?.superclasses?.firstOrNull() ?: break
    return path.asReversed()
  }

  /** The child of [node] on the way to [name], or null when [name] is [node] itself. */
  fun childToward(node: String, name: String): String? {
    val path = primaryPath(name)
    return path.getOrNull(path.indexOf(node) + 1)
  }
}

/**
 * How the Explorer is expanded, kept outside the tab so it survives moving between panes. Count
 * edits made in sandbox mode live here too.
 */
@Stable
internal class ExplorerState {
  internal val splits = mutableStateMapOf<Node, Split>(Node(ROOT) to Split.BySubtype)
  var includeNonReal by mutableStateOf(false)

  /** Sandbox count edits and summoned components, by type. */
  val counts = mutableStateMapOf<String, Int>()
}

/** How an Explorer node is subdivided. */
internal sealed interface Split {
  data object BySubtype : Split

  data class ByDependency(val key: String) : Split
}

/** A node: present components of [className] whose dependencies match [fixed]. */
internal data class Node(val className: String, val fixed: Map<String, String> = emptyMap())

private sealed interface ExplorerLine {
  val depth: Int

  data class Group(
      override val depth: Int,
      val node: Node,
      val count: Int,
      val split: Split?,
  ) : ExplorerLine

  data class Leaf(override val depth: Int, val component: Specimen.PresentComponent) : ExplorerLine
}

/**
 * Present component types and counts as a tree, with abstract types as inner nodes. A node expands
 * by concrete subtype; clicking one of its dependency names regroups it by that dependency instead.
 * Either way the node's total is unchanged.
 */
@Composable
internal fun ExplorerTab(
    state: ExplorerState,
    sandbox: Boolean,
    selectedClass: String?,
) {
  val specimen = LocalSpecimen.current
  val palette = LocalPalette.current
  val tree = remember(specimen) { TypeTree(specimen) }
  val splits = state.splits
  val counts = state.counts
  val present =
      specimen.components
          .filter { state.includeNonReal || !it.nonReal }
          .map { it.copy(count = counts[it.type] ?: it.count) } +
          counts.keys
              .filter { type -> specimen.components.none { it.type == type } }
              .map { type ->
                Specimen.PresentComponent(
                    type,
                    counts.getValue(type),
                    type.substringBefore('<'),
                    false,
                    false,
                    emptyMap(),
                )
              }
  val lines = buildList { explore(tree, splits, Node(ROOT), present.filter { it.count > 0 }, 0) }

  Column(Modifier.fillMaxSize().padding(palette.spacing)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(
          state.includeNonReal,
          { state.includeNonReal = it },
          Modifier.size(28.dp),
          colors =
              CheckboxDefaults.colors(
                  checkedColor = palette.accent,
                  uncheckedColor = palette.dimText,
              ),
      )
      Text("Include non-Real components", style = palette.body.copy(color = palette.dimText))
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
      items(lines) { line ->
        when (line) {
          is ExplorerLine.Group ->
              GroupLine(
                  tree,
                  line,
                  selected = line.node.className == selectedClass,
                  onToggle = {
                    if (line.split == null) splits[line.node] = Split.BySubtype
                    else splits.remove(line.node)
                  },
                  onRegroup = { key -> splits[line.node] = Split.ByDependency(key) },
              )
          is ExplorerLine.Leaf ->
              LeafLine(
                  line,
                  onChange =
                      if (sandbox) {
                        { change ->
                          counts[line.component.type] =
                              (line.component.count + change).coerceAtLeast(0)
                        }
                      } else {
                        null
                      },
              )
        }
      }
    }
    if (sandbox) Summoner { type, count -> counts[type] = (counts[type] ?: 0) + count }
  }
}

private const val ROOT = "Component"

private fun MutableList<ExplorerLine>.explore(
    tree: TypeTree,
    splits: Map<Node, Split>,
    node: Node,
    components: List<Specimen.PresentComponent>,
    depth: Int,
) {
  val split = splits[node]
  add(ExplorerLine.Group(depth, node, components.sumOf { it.count }, split))
  when (split) {
    null -> {}
    is Split.ByDependency -> {
      val groups = components.groupBy { it.dependencies[split.key] ?: "?" }
      for ((value, members) in groups.entries.sortedBy { it.key }) {
        explore(
            tree,
            splits,
            node.copy(fixed = node.fixed + (split.key to value)),
            members,
            depth + 1,
        )
      }
    }
    is Split.BySubtype -> {
      val byChild = components.groupBy { tree.childToward(node.className, it.className) }
      for ((child, members) in byChild.entries.sortedBy { it.key ?: "" }) {
        if (child == null || members.singleOrNull()?.className == child) {
          // A component alone in its own class needs no group of its own.
          members.sortedBy { it.type }.forEach { add(ExplorerLine.Leaf(depth + 1, it)) }
        } else {
          explore(tree, splits, Node(child, node.fixed), members, depth + 1)
        }
      }
    }
  }
}

@Composable
private fun GroupLine(
    tree: TypeTree,
    line: ExplorerLine.Group,
    selected: Boolean,
    onToggle: () -> Unit,
    onRegroup: (String) -> Unit,
) {
  val palette = LocalPalette.current
  val info = tree.info(line.node.className)
  ExplorerRow(line.depth, line.count, selected) {
    Text(
        if (line.split == null) "▸" else "▾",
        Modifier.width(14.dp).clickable(onClick = onToggle),
        style = palette.mono.copy(color = palette.dimText),
    )
    val base = info?.baseType ?: line.node.className
    val arguments = typeArguments(base)
    val keys = info?.dependencyKeys.orEmpty()
    val mono =
        palette.mono.copy(color = if (info?.abstract == false) palette.text else palette.admin)
    ClassLinkedText(line.node.className, color = mono.color)
    if (arguments.isNotEmpty()) {
      Text("<", style = mono)
      arguments.forEachIndexed { i, argument ->
        if (i > 0) Text(", ", style = mono)
        val key = keys.getOrNull(i).takeIf { keys.size == arguments.size }
        val fixed = key?.let { line.node.fixed[it] }
        when {
          fixed != null -> ClassLinkedText(fixed, color = palette.mars)
          key != null -> {
            Text(
                "↳",
                Modifier.clickable { onRegroup(key) },
                style = mono.copy(color = palette.accent),
            )
            ClassLinkedText(argument, color = mono.color)
          }
          else -> ClassLinkedText(argument, color = mono.color)
        }
      }
      Text(">", style = mono)
    }
  }
}

@Composable
private fun LeafLine(line: ExplorerLine.Leaf, onChange: ((Int) -> Unit)?) {
  val palette = LocalPalette.current
  val editable = onChange != null && !line.component.nonReal && !line.component.system
  ExplorerRow(
      line.depth,
      line.component.count,
      selected = false,
      trailing = {
        if (editable) {
          Stepper("−") { onChange(-1) }
          Stepper("+") { onChange(1) }
        }
      },
  ) {
    Spacer(Modifier.width(14.dp))
    PetsText(
        line.component.type,
        color = if (line.component.nonReal) palette.dimText else palette.text,
    )
  }
}

@Composable
private fun ExplorerRow(
    depth: Int,
    count: Int,
    selected: Boolean,
    trailing: @Composable () -> Unit = {},
    label: @Composable () -> Unit,
) {
  val palette = LocalPalette.current
  Row(
      Modifier.fillMaxWidth()
          .height(22.dp)
          .background(if (selected) palette.accent.copy(alpha = 0.12f) else palette.surface)
          .padding(start = (depth * 16).dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { label() }
    trailing()
    Text(
        "$count",
        Modifier.width(52.dp),
        style = palette.mono.copy(color = palette.text, textAlign = TextAlign.End),
    )
  }
}

@Composable
private fun Stepper(symbol: String, onClick: () -> Unit) {
  val palette = LocalPalette.current
  Box(
      Modifier.padding(start = 3.dp)
          .size(17.dp)
          .border(palette.borderWidth, palette.line, RoundedCornerShape(3.dp))
          .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Text(symbol, style = palette.mono.copy(color = palette.accent))
  }
}

/** Sandbox only: type a component and summon some number of it. */
@Composable
private fun Summoner(onSummon: (String, Int) -> Unit) {
  val palette = LocalPalette.current
  var type by remember { mutableStateOf("") }
  var count by remember { mutableStateOf("1") }
  Row(
      Modifier.fillMaxWidth().padding(top = palette.spacing / 2),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("SUMMON", style = palette.label)
    Box(Modifier.width(52.dp)) {
      BasicTextField(
          count,
          { count = it.filter(Char::isDigit).take(3) },
          textStyle = palette.mono.copy(color = palette.text),
          cursorBrush = SolidColor(palette.accent),
          singleLine = true,
          decorationBox = { Underlined(it) },
      )
    }
    Box(Modifier.weight(1f)) {
      BasicTextField(
          type,
          { type = it },
          textStyle = palette.mono.copy(color = palette.text),
          cursorBrush = SolidColor(palette.accent),
          singleLine = true,
          decorationBox = { Underlined(it) },
      )
    }
    Stepper("+") {
      val n = count.toIntOrNull() ?: 0
      if (type.isNotBlank() && n > 0) onSummon(type.trim(), n)
    }
  }
}

/** The top-level arguments of a type expression such as `OwnedTile<Owner, Area>`. */
private fun typeArguments(type: String): List<String> {
  val open = type.indexOf('<')
  if (open < 0 || !type.endsWith(">")) return emptyList()
  val inner = type.substring(open + 1, type.length - 1)
  val arguments = mutableListOf<String>()
  var depth = 0
  var start = 0
  inner.forEachIndexed { i, c ->
    when (c) {
      '<' -> depth++
      '>' -> depth--
      ',' ->
          if (depth == 0) {
            arguments += inner.substring(start, i).trim()
            start = i + 1
          }
    }
  }
  arguments += inner.substring(start).trim()
  return arguments
}
