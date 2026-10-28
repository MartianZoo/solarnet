package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.fromKeyword
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.zIndex
import dev.martianzoo.tfm.web.playground.resources.Res
import dev.martianzoo.tfm.web.playground.resources.prototype
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.Font

private val LocalCardImageDirectory = compositionLocalOf { "classes" }
private val LocalCardActionBoxes =
    compositionLocalOf<Map<String, List<CardActionBox>>> { emptyMap() }

/**
 * Selecting cards and a reorderable hand share a scrolling row. Played cards fill eight columns:
 * corporations, Preludes, an automated stack, and five columns of active cards.
 */
@Composable
internal fun PlayedCards(
    player: Specimen.PlayerState,
    publishedText: Boolean,
    onPublishedText: (Boolean) -> Unit,
    handOrder: List<String>,
    onHandOrder: (List<String>) -> Unit,
    cards: Prompt.Cards?,
    onToggleCard: (String) -> Unit,
) {
  val palette = LocalPalette.current
  val actionBoxes by
      produceState<Map<String, List<CardActionBox>>>(emptyMap()) {
        value = loadCardActionBoxes()
      }
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val gap = maxWidth * (8f / 1472f)
    val width = (maxWidth - gap * 7) / 8
    CompositionLocalProvider(
        LocalCardImageDirectory provides if (publishedText) "classes-pubtext" else "classes",
        LocalCardActionBoxes provides actionBoxes,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(gap * 3)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
              publishedText,
              onPublishedText,
              Modifier.width(26.dp).height(26.dp),
              colors =
                  CheckboxDefaults.colors(
                      checkedColor = palette.accent,
                      uncheckedColor = palette.dimText,
                  ),
          )
          Text(
              "Published card text",
              Modifier.clickable { onPublishedText(!publishedText) },
              style = palette.label,
          )
        }
        if (player.hand.isNotEmpty() || player.selecting.isNotEmpty()) {
          val hand =
              handOrder.filter { it in player.hand } + player.hand.filterNot { it in handOrder }
          HandRow(player, hand, onHandOrder, width, gap, cards, onToggleCard)
        }
        PlayedRow(player, width, gap)
      }
    }
  }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun HandRow(
    player: Specimen.PlayerState,
    hand: List<String>,
    onHandOrder: (List<String>) -> Unit,
    width: Dp,
    gap: Dp,
    cards: Prompt.Cards?,
    onToggleCard: (String) -> Unit,
) {
  val palette = LocalPalette.current
  val currentHand by rememberUpdatedState(hand)
  val reorder by rememberUpdatedState(onHandOrder)
  val pitch = with(LocalDensity.current) { (width + gap).toPx() }
  var dragging by remember(player.name, player.hand) { mutableStateOf<String?>(null) }
  var displacement by remember(player.name, player.hand) { mutableFloatStateOf(0f) }
  Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(gap),
      verticalAlignment = Alignment.Top,
  ) {
    if (player.selecting.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Text("SELECTING", style = palette.label)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
          player.selecting.forEach { name ->
            key(name) {
              CardImage(
                  name,
                  null,
                  false,
                  player.name,
                  width,
                  selectedColor =
                      if (cards?.fromHand == false && name in cards.chosen) {
                        if (cards?.drawing == true) Color(0xFF4DAA67) else Color(0xFFC54B4B)
                      } else null,
                  onSelect =
                      if (cards?.fromHand == false && name in cards.available)
                          ({ onToggleCard(name) })
                      else null,
                  unproven = name in cards?.unproven.orEmpty(),
              )
            }
          }
        }
      }
      Box(Modifier.width(palette.borderWidth * 2).height(width).background(palette.line))
    }
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
      Text("HAND", style = palette.label)
      Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        hand.forEach { name ->
          key(name) {
            CardImage(
                name,
                null,
                false,
                player.name,
                width,
                Modifier.zIndex(if (dragging == name) 1f else 0f)
                    .offset { IntOffset(if (dragging == name) displacement.roundToInt() else 0, 0) }
                    .pointerHoverIcon(PointerIcon.fromKeyword("grab"))
                    .pointerInput(name, player.hand, pitch) {
                      detectHorizontalDragGestures(
                          onDragStart = {
                            dragging = name
                            displacement = 0f
                          },
                          onDragEnd = {
                            dragging = null
                            displacement = 0f
                          },
                          onDragCancel = {
                            dragging = null
                            displacement = 0f
                          },
                      ) { change, distance ->
                        change.consume()
                        displacement += distance
                        val from = currentHand.indexOf(name)
                        val to =
                            (from + (displacement / pitch).roundToInt()).coerceIn(
                                0,
                                currentHand.lastIndex,
                            )
                        if (from != to) {
                          reorder(currentHand.toMutableList().apply { add(to, removeAt(from)) })
                          displacement -= (to - from) * pitch
                        }
                      }
                    },
                selectedColor =
                    if (cards?.fromHand == true && name in cards.chosen) Color(0xFFC54B4B)
                    else null,
                onSelect =
                    if (cards?.fromHand == true && name in cards.available) ({ onToggleCard(name) })
                    else null,
                action = "PlayProject",
                unproven = name in cards?.unproven.orEmpty(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PlayedRow(player: Specimen.PlayerState, width: Dp, gap: Dp) {
  Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(gap),
      verticalAlignment = Alignment.Top,
  ) {
    for (kind in Specimen.CardKind.entries) {
      val cards = player.played.filter { it.kind == kind }
      when (kind) {
        Specimen.CardKind.Automated -> CardStack(cards, player.name, width)
        Specimen.CardKind.Active ->
            Column(verticalArrangement = Arrangement.spacedBy(gap * 2)) {
              cards.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                  row.forEach { card -> key(card.name) { CardImage(card, player.name, width) } }
                }
              }
            }
        else ->
            Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(gap * 2)) {
              cards.forEach { card -> key(card.name) { CardImage(card, player.name, width) } }
            }
      }
    }
  }
}

@Composable
private fun CardStack(cards: List<Specimen.PlayedCard>, player: String, width: Dp) {
  if (cards.isEmpty()) {
    Box(Modifier.width(width))
    return
  }
  // The next card's top inset leaves the preceding title visible at this tighter pitch.
  val step = width * (140f / 552f)
  Box(Modifier.width(width).height(step * (cards.size - 1) + width * (672f / 552f))) {
    cards.forEachIndexed { index, card ->
      key(card.name) {
        val hover = remember { MutableInteractionSource() }
        val hovered by hover.collectIsHoveredAsState()
        Box(Modifier.offset(y = step * index).hoverable(hover)) {
          CardImage(card, player, width)
          if (hovered) {
            Popup(
                popupPositionProvider =
                    remember {
                      object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize,
                        ) =
                            IntOffset(
                                (anchorBounds.left - popupContentSize.width - 8).coerceAtLeast(0),
                                anchorBounds.top.coerceIn(
                                    0,
                                    (windowSize.height - popupContentSize.height).coerceAtLeast(0),
                                ),
                            )
                      }
                    }
            ) {
              CardImage(card, player, width)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CardImage(
    card: Specimen.PlayedCard,
    player: String,
    width: Dp,
    modifier: Modifier = Modifier,
) =
    CardImage(
        card.name,
        card.resources,
        card.actionUsed,
        player,
        width,
        modifier,
        card.resourceType,
        actionBoxes = LocalCardActionBoxes.current[card.name].orEmpty(),
    )

@Composable
private fun CardImage(
    name: String,
    resources: Int?,
    actionUsed: Boolean,
    player: String,
    width: Dp,
    modifier: Modifier = Modifier,
    resourceType: String? = null,
    selectedColor: Color? = null,
    onSelect: (() -> Unit)? = null,
    action: String? = null,
    unproven: Boolean = false,
    actionBoxes: List<CardActionBox> = emptyList(),
) {
  val palette = LocalPalette.current
  val choices = LocalPlayChoices.current
  val alternateName = if (name.startsWith("Fake")) name.removePrefix("Fake") else "Fake$name"
  val directory = LocalCardImageDirectory.current
  val image =
      rememberGameImage("$directory/$name.png")
          ?: rememberGameImage("$directory/$alternateName.png")
  val height = if (image != null) width * image.height / image.width else width * (672f / 552f)
  val cardModifier = modifier.width(width).height(height)
  val selectResourceCard =
      if (resourceType == null) null else choices?.resourceCard(name, resourceType, resources)
  val availableActions =
      if (image == null || actionUsed || choices == null) emptyList()
      else actionBoxes.mapNotNull { box -> choices.cardAction(name, box.slot)?.let { box to it } }
  val projectClick = if (action == "PlayProject") choices?.playProject(name) else null
  Box(
      if (onSelect == null && availableActions.isEmpty() && selectResourceCard == null)
          if (action == "PlayProject" && projectClick == null) cardModifier
          else
              choiceModifier(
                  listOf(name, "Class<$name>"),
                  cardModifier,
                  if (action == "PlayProject") null else action,
                  onChoice = projectClick,
              )
      else if (onSelect == null) cardModifier
      else
          cardModifier
              .pointerHoverIcon(PointerIcon.Hand)
              .clickable(onClick = onSelect)
              .then(if (selectedColor == null) Modifier else Modifier.border(4.dp, selectedColor))
  ) {
    if (image != null) {
      Image(image, name, Modifier.fillMaxSize())
    } else {
      Box(
          Modifier.fillMaxSize().background(palette.raised).border(1.dp, palette.line).padding(8.dp)
      ) {
        Text(name, style = palette.mono)
      }
    }
    if (unproven) {
      Text(
          "*",
          Modifier.align(Alignment.TopStart).background(palette.raised).padding(horizontal = 3.dp),
          style = palette.mono.copy(color = palette.mars),
      )
    }
    if (actionUsed) {
      ActorSquare(
          player,
          playerCubeSize,
          Modifier.align(Alignment.TopCenter).offset(y = height / 3 - playerCubeSize / 2),
      )
    }
    if (image != null && resources != null && resources != 0) {
      // Original renderer: Prototype 22 CSS px at 2x; the badge is 14 px from the canvas edge.
      // Leave the printed zero untouched at zero. Replace only its numeral area at other counts.
      val scale = width.value / image.width
      val font = FontFamily(Font(Res.font.prototype))
      val measurer = rememberTextMeasurer()
      val number =
          measurer.measure(
              resources.toString(),
              TextStyle(fontFamily = font, fontSize = (44 * scale).sp, color = Color(0xFF020202)),
          )
      Canvas(Modifier.fillMaxSize()) {
        val pixel = size.width / image.width
        drawRect(
            brush =
                Brush.horizontalGradient(
                    listOf(Color(0xFFDDDDDD), Color(0xFF666666)),
                    36 * pixel,
                    196 * pixel,
                ),
            topLeft = Offset(43 * pixel, size.height - 91 * pixel),
            size = Size(63 * pixel, 48 * pixel),
        )
        drawText(
            number,
            topLeft =
                Offset(
                    103 * pixel - number.size.width,
                    size.height - 48 * pixel - number.firstBaseline,
                ),
        )
      }
    }
    if (resourceType != null) {
      if (selectResourceCard != null) {
        // The badge is small; keep twelve card pixels between it and the green hitbox.
        Box(
            Modifier.align(Alignment.BottomStart)
                .offset(x = width * (24f / 552f), y = -(width * (29f / 552f)))
                .width(width * (94f / 552f))
                .height(width * (72f / 552f))
                .border(1.dp, Color(0xFF4DAA67))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(onClick = selectResourceCard)
        )
      } else {
        Box(
            choiceModifier(
                listOf(
                    resourceType,
                    "Class<$resourceType>",
                    "$resourceType<$name>",
                    "CardResource<$name>",
                ),
                Modifier.align(Alignment.BottomStart)
                    .offset(x = width * (36f / 552f), y = -(width * (41f / 552f)))
                    .width(width * (70f / 552f))
                    .height(width * (48f / 552f)),
            )
        )
      }
    }
    for ((box, select) in availableActions) {
      // The survey bounds enclose the print; eight source pixels keep the outline off the artwork.
      val left = (box.x - 8).coerceAtLeast(0)
      val top = (box.y - 8).coerceAtLeast(0)
      val right = (box.x + box.width + 8).coerceAtMost(box.imageWidth)
      val bottom = (box.y + box.height + 8).coerceAtMost(box.imageHeight)
      Box(
          Modifier.offset(
                  x = width * (left.toFloat() / box.imageWidth),
                  y = height * (top.toFloat() / box.imageHeight),
              )
              .width(width * ((right - left).toFloat() / box.imageWidth))
              .height(height * ((bottom - top).toFloat() / box.imageHeight))
              .border(1.5.dp, Color(0xFFE04444).copy(alpha = 0.85f))
              .pointerHoverIcon(PointerIcon.Hand)
              .clickable(onClick = select)
      )
    }
  }
}
