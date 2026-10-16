package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode

internal val LocalOpenClass =
    staticCompositionLocalOf<(String) -> Unit> { error("No class navigator") }
internal val LocalClassNames = staticCompositionLocalOf<Set<String>> { emptySet() }

internal val LocalUseTrays = staticCompositionLocalOf { false }

private val classWord = Regex("[A-Za-z][A-Za-z0-9_]*")

@Composable
private fun linkedPets(
    text: String,
    icons: Set<String> = emptySet(),
    linkClasses: Boolean = true,
): AnnotatedString {
  val classNames = LocalClassNames.current
  val openClass = LocalOpenClass.current
  val textColor = LocalPalette.current.text
  return buildAnnotatedString {
    var offset = 0
    for (match in classWord.findAll(text)) {
      append(text.substring(offset, match.range.first))
      val name = match.value
      if (name in icons) {
        appendInlineContent(name, name)
      } else if (linkClasses && name in classNames) {
        withLink(
            LinkAnnotation.Clickable(
                tag = name,
                styles = TextLinkStyles(style = SpanStyle(color = textColor)),
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
      color = color ?: palette.text,
      maxLines = maxLines,
  )
}

/** A text segment; [player] is set only when this expression lost that player argument. */
internal data class TrayRun(val text: String, val player: String?)

private val expressionWithArguments =
    Regex("(?:[A-Za-z][A-Za-z0-9_]*@|@)?[A-Za-z][A-Za-z0-9_]*[<(]")

/** Each complete type expression gets a tray only if its player arguments were removed. */
internal fun petsPieces(
    text: String,
    players: Collection<String>,
    source: PetNode? = null,
): List<TrayRun> {
  if (players.isEmpty()) return listOf(TrayRun(text, null))
  // Effect variables are scoped by the whole effect, not by an isolated expression. Keep the
  // original nodes when available; parsing their displayed fragments can lose that scope.
  val expressions = source?.descendantsOfType<Expression>()?.associateBy { it.toString() }.orEmpty()
  return buildList {
    var offset = 0
    var scanned = 0
    for (match in expressionWithArguments.findAll(text)) {
      if (match.range.first < scanned) continue
      var end = closingDelimiter(text, match.range.last) ?: continue
      if (text.getOrNull(end) == '(') end = closingDelimiter(text, end) ?: continue
      val spelling = text.substring(match.range.first, end)
      val expression =
          expressions[spelling]
              ?: runCatching { Parsing.parse<Expression>(spelling) }.getOrNull()
              ?: continue
      scanned = end
      val omitted = mutableSetOf<String>()
      fun omitPlayers(expression: Expression): Expression {
        val arguments =
            expression.arguments
                .filterNot {
                  (it.simple && it.toString() in players).also { remove ->
                    if (remove) omitted += it.toString()
                  }
                }
                .map(::omitPlayers)
        return expression.copy(
            arguments = arguments,
            argumentsSpecified =
                if (expression.arguments.isEmpty()) expression.argumentsSpecified
                else arguments.isNotEmpty(),
        )
      }
      val shortened = omitPlayers(expression)
      val player = omitted.singleOrNull() ?: continue
      if (match.range.first > offset) add(TrayRun(text.substring(offset, match.range.first), null))
      add(TrayRun(shortened.toString(), player))
      offset = end
    }
    if (offset < text.length) add(TrayRun(text.substring(offset), null))
  }
}

/** Returns the position after a balanced argument list or refinement. */
private fun closingDelimiter(text: String, start: Int): Int? {
  val opening = text[start]
  val closing = if (opening == '<') '>' else ')'
  var depth = 0
  for (i in start until text.length) {
    if (text[i] == opening) depth++
    if (text[i] == closing && --depth == 0) return i + 1
  }
  return null
}

/** Draws [text] in the monospace face, optionally shortening player arguments onto trays. */
@Composable
internal fun PetsText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    icons: Boolean = false,
    linkClasses: Boolean = true,
    source: PetNode? = null,
) {
  Row(modifier.padding(horizontal = 3.dp), verticalAlignment = Alignment.Bottom) {
    val players = if (LocalUseTrays.current) LocalSpecimen.current.playerNames else emptyList()
    for (piece in petsPieces(text, players, source)) {
      TrayText(piece, color, icons, linkClasses)
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
      color = color ?: palette.text,
      maxLines = 1,
      softWrap = false,
  )
}

/**
 * One tray run: text close above a thin underline in the player's color, ticked up at both ends.
 */
@Composable
internal fun TrayText(
    run: TrayRun,
    color: Color? = null,
    icons: Boolean = false,
    linkClasses: Boolean = true,
) {
  val palette = LocalPalette.current
  var baseline by remember { mutableFloatStateOf(0f) }
  val trayColor = run.player?.let { actorColor(it) }
  val classNames = LocalClassNames.current
  val players = LocalSpecimen.current.playerNames
  val openClass = LocalOpenClass.current
  val inlineIcons = mutableMapOf<String, InlineTextContent>()
  if (icons) {
    val names =
        classWord
            .findAll(run.text)
            .map { it.value }
            .filter { it in classNames || it in players }
            .distinct()
            .toList()
    for (name in names) {
      key(name) {
        if (name in players || name == "SoloOpponent") {
          inlineIcons[name] =
              InlineTextContent(Placeholder(1.32.em, 1.32.em, PlaceholderVerticalAlign.Center)) {
                Tooltip(name) {
                  ActorSquare(
                      name,
                      modifier =
                          Modifier.fillMaxSize()
                              .then(
                                  if (linkClasses) Modifier.clickable { openClass(name) }
                                  else Modifier
                              ),
                  )
                }
              }
        } else {
          val image = rememberClassIcon(name)
          if (image != null) {
            inlineIcons[name] =
                InlineTextContent(
                    Placeholder(
                        (1.32f * image.width / image.height).em,
                        1.32.em,
                        PlaceholderVerticalAlign.Center,
                    )
                ) {
                  Tooltip(name) {
                    Image(
                        image,
                        name,
                        Modifier.fillMaxSize()
                            .then(
                                if (linkClasses) Modifier.clickable { openClass(name) }
                                else Modifier
                            ),
                    )
                  }
                }
          }
        }
      }
    }
  }
  Text(
      linkedPets(run.text, inlineIcons.keys, linkClasses),
      Modifier.padding(horizontal = if (trayColor == null) 0.dp else 3.dp).drawWithContent {
        drawContent()
        if (trayColor == null || baseline == 0f) return@drawWithContent
        val reach = 2.5.dp.toPx()
        val y = baseline + 4.5.dp.toPx()
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
      color = color ?: palette.text,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Clip,
      inlineContent = inlineIcons,
      onTextLayout = { baseline = it.firstBaseline },
  )
}
