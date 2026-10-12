package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

internal val LocalOpenClass =
    staticCompositionLocalOf<(String) -> Unit> { error("No class navigator") }
internal val LocalClassNames = staticCompositionLocalOf<Set<String>> { emptySet() }

private val classWord = Regex("[A-Za-z][A-Za-z0-9_]*")

@Composable
private fun linkedPets(text: String): AnnotatedString {
  val classNames = LocalClassNames.current
  val openClass = LocalOpenClass.current
  val accent = LocalPalette.current.accent
  return buildAnnotatedString {
    var offset = 0
    for (match in classWord.findAll(text)) {
      append(text.substring(offset, match.range.first))
      val name = match.value
      if (name in classNames) {
        withLink(
            LinkAnnotation.Clickable(
                tag = name,
                styles = TextLinkStyles(style = SpanStyle(color = accent)),
                linkInteractionListener = { openClass(name) },
            )
        ) {
          append(name)
        }
      } else {
        append(name)
      }
      offset = match.range.last + 1
    }
    append(text.substring(offset))
  }
}

/** Class names in raw Pets text open the in-workspace class inspector. */
@Composable
internal fun ClassLinkedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    style: TextStyle? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
  val palette = LocalPalette.current
  Text(
      linkedPets(text),
      modifier,
      style = style ?: palette.mono,
      color = color ?: palette.admin,
      maxLines = maxLines,
  )
}

/**
 * A run of Pets text drawn on one tray. When every player named as a dependency argument is the
 * same [player], those arguments are omitted from [text] and the tray carries that player's color.
 */
internal data class TrayRun(val text: String, val player: String?)

/** Pets text split into tray runs and the plain keywords or arrows between them. */
internal sealed interface PetsPiece {
  data class Run(val run: TrayRun) : PetsPiece

  data class Joiner(val text: String) : PetsPiece
}

/** Splits [text] at top-level `THEN`, giving each side its own tray. */
internal fun petsPieces(text: String, players: Collection<String>): List<PetsPiece> {
  val parts = splitTopLevel(text, " THEN ")
  return parts.flatMapIndexed { i, part ->
    listOfNotNull(
        PetsPiece.Joiner("THEN").takeIf { i > 0 },
        PetsPiece.Run(shortenPlayers(part, players)),
    )
  }
}

private fun splitTopLevel(text: String, separator: String): List<String> {
  val parts = mutableListOf<String>()
  var depth = 0
  var start = 0
  var i = 0
  while (i < text.length) {
    when (text[i]) {
      '<',
      '(',
      '{' -> depth++
      '>',
      ')',
      '}' -> depth--
    }
    if (depth == 0 && text.startsWith(separator, i)) {
      parts += text.substring(start, i)
      i += separator.length
      start = i
    } else {
      i++
    }
  }
  parts += text.substring(start)
  return parts
}

/**
 * Omits player-name dependency arguments from [expression] when they all name the same player:
 * `ProjectCard<Yellow, Hand>!` becomes `ProjectCard<Hand>!` on a Yellow tray. A bare player name
 * keeps its text and gets that player's tray. Mixed players leave the text unchanged.
 */
internal fun shortenPlayers(expression: String, players: Collection<String>): TrayRun {
  if (expression.trim() in players) return TrayRun(expression, expression.trim())
  val arguments = mutableListOf<IntRange>()
  var depth = 0
  var i = 0
  while (i < expression.length) {
    val c = expression[i]
    when {
      c == '<' -> {
        depth++
        i++
      }
      c == '>' -> {
        depth--
        i++
      }
      c.isLetter() -> {
        var end = i
        while (
            end < expression.length && (expression[end].isLetterOrDigit() || expression[end] == '_')
        ) end++
        val before = expression.substring(0, i).trimEnd().lastOrNull()
        if (
            depth > 0 && (before == '<' || before == ',') && expression.substring(i, end) in players
        ) {
          arguments += i until end
        }
        i = end
      }
      else -> i++
    }
  }
  val named = arguments.map { expression.substring(it) }.toSet()
  if (named.size != 1) return TrayRun(expression, null)
  var shortened = expression
  for (range in arguments.asReversed()) {
    val after = shortened.substring(range.last + 1)
    val before = shortened.substring(0, range.first)
    shortened =
        when {
          after.startsWith(", ") -> before + after.removePrefix(", ")
          before.endsWith(", ") -> before.removeSuffix(", ") + after
          else -> before.removeSuffix("<") + after.removePrefix(">")
        }
  }
  return TrayRun(shortened, named.single())
}

/** Draws [text] in the monospace face, shortened and on player trays as [shortenPlayers] says. */
@Composable
internal fun PetsText(text: String, modifier: Modifier = Modifier, color: Color? = null) {
  val players = LocalSpecimen.current.playerNames
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    for (piece in petsPieces(text, players)) {
      when (piece) {
        is PetsPiece.Joiner -> PlainPets(piece.text, color)
        is PetsPiece.Run -> TrayText(piece.run, color)
      }
    }
  }
}

@Composable
internal fun PlainPets(text: String, color: Color? = null) {
  val palette = LocalPalette.current
  Text(
      text,
      Modifier.padding(horizontal = 3.dp),
      style = palette.mono,
      color = color ?: palette.admin,
      maxLines = 1,
      softWrap = false,
  )
}

/**
 * One tray run: text close above a thin underline in the player's color, ticked up at both ends.
 */
@Composable
internal fun TrayText(run: TrayRun, color: Color? = null) {
  val palette = LocalPalette.current
  var baseline by remember { mutableFloatStateOf(0f) }
  val trayColor = run.player?.let { actorColor(it) }
  Text(
      linkedPets(run.text),
      Modifier.padding(horizontal = 3.dp).drawBehind {
        if (trayColor == null || baseline == 0f) return@drawBehind
        val reach = 2.5.dp.toPx()
        val y = baseline + 2.5.dp.toPx()
        val tick = 2.5.dp.toPx()
        val stroke = 1.2.dp.toPx()
        drawLine(
            trayColor,
            Offset(-reach, y),
            Offset(size.width + reach, y),
            stroke,
            StrokeCap.Round,
        )
        drawLine(trayColor, Offset(-reach, y), Offset(-reach, y - tick), stroke, StrokeCap.Round)
        drawLine(
            trayColor,
            Offset(size.width + reach, y),
            Offset(size.width + reach, y - tick),
            stroke,
            StrokeCap.Round,
        )
      },
      style = palette.mono,
      color = color ?: palette.admin,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Clip,
      onTextLayout = { baseline = it.firstBaseline },
  )
}
