package dev.martianzoo.tfm.web.playground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import dev.martianzoo.tfm.web.playground.resources.Res
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.jetbrains.compose.resources.decodeToImageBitmap

internal val LocalSpecimen = compositionLocalOf<Specimen> { error("no specimen") }

internal val LocalCardNames = compositionLocalOf<Set<String>> { emptySet() }

internal val playerCubeSize = 21.dp

/**
 * A plain physical surface for one region: a softly shadowed panel with the theme's border and
 * corners. Regions deliberately have no uniform heading bar.
 */
@Composable
internal fun Surface(
    modifier: Modifier = Modifier,
    color: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalPalette.current
  val shape = RoundedCornerShape(palette.cornerRadius)
  Box(
      modifier
          .fillMaxSize()
          .shadow(palette.shadow, shape, clip = false)
          .clip(shape)
          .background(color ?: palette.surface)
          .border(palette.borderWidth, palette.line, shape),
      content = content,
  )
}

/** A player's (or Admin's) identity as a sharp-edged, three-face cube. */
@Composable
internal fun ActorSquare(actor: String?, size: Dp = 11.dp, modifier: Modifier = Modifier) {
  val color = actorColor(actor)
  Canvas(modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val outline =
        Path().apply {
          moveTo(w * 0.04f, h * 0.22f)
          lineTo(w * 0.24f, h * 0.04f)
          lineTo(w * 0.96f, h * 0.04f)
          lineTo(w * 0.96f, h * 0.78f)
          lineTo(w * 0.76f, h * 0.96f)
          lineTo(w * 0.04f, h * 0.96f)
          close()
        }
    val top =
        Path().apply {
          moveTo(w * 0.04f, h * 0.22f)
          lineTo(w * 0.24f, h * 0.04f)
          lineTo(w * 0.96f, h * 0.04f)
          lineTo(w * 0.76f, h * 0.22f)
          close()
        }
    val right =
        Path().apply {
          moveTo(w * 0.76f, h * 0.22f)
          lineTo(w * 0.96f, h * 0.04f)
          lineTo(w * 0.96f, h * 0.78f)
          lineTo(w * 0.76f, h * 0.96f)
          close()
        }
    drawPath(outline, color)
    drawPath(top, Color.White.copy(alpha = 0.3f))
    drawPath(right, Color.Black.copy(alpha = 0.28f))
    drawPath(outline, Color.Black, style = Stroke(width = 0.5.dp.toPx()))
  }
}

/** The eight-pointed star marking a Signal in the event log. */
@Composable
internal fun SignalStar(color: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
  Canvas(modifier.size(size)) {
    val c = Offset(this.size.width / 2, this.size.height / 2)
    val outer = this.size.minDimension / 2
    val inner = outer * 0.42f
    val path = Path()
    for (i in 0 until 16) {
      val r = if (i % 2 == 0) outer else inner
      val angle = PI * i / 8 - PI / 2
      val p = Offset(c.x + r * cos(angle).toFloat(), c.y + r * sin(angle).toFloat())
      if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color)
  }
}

// Loading belongs to the shared cache, so a disappearing preview row cannot cancel it.
private val imageScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
private val imageCache = mutableMapOf<String, Deferred<ImageBitmap?>>()

private fun loadGameImage(path: String): Deferred<ImageBitmap?> =
    imageCache.getOrPut(path) {
      imageScope.async {
        runCatching { Res.readBytes("files/images/$path").decodeToImageBitmap() }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
      }
    }

/** Loads game art, trying paths in order; null while loading or if all are absent. */
@Composable
internal fun rememberGameImage(vararg paths: String): ImageBitmap? {
  val candidates = paths.toList()
  var image by remember(candidates) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(candidates) {
    image = candidates.firstNotNullOfOrNull { loadGameImage(it).await() }
  }
  return image
}

/** Prefer small icon artwork, falling back to the full-size class image when absent. */
@Composable
internal fun rememberClassIcon(name: String): ImageBitmap? =
    when {
      name == "Production" || name in LocalCardNames.current -> null
      name == "MC" -> rememberGameImage("classes/MC.png")
      else -> rememberGameImage("classes/30px/$name.png", "classes/$name.png")
    }

/** A small game icon from the hosted icon set, with a lettered stand-in when it has none. */
@Composable
internal fun GameIcon(name: String, size: Dp, modifier: Modifier = Modifier) {
  val image = rememberClassIcon(name)
  if (image != null) {
    Image(image, name, modifier.size(size), contentScale = ContentScale.Fit)
  } else {
    val palette = LocalPalette.current
    Box(
        modifier.size(size).clip(RoundedCornerShape(size / 5)).background(Color(0xFFD9B44A)),
        contentAlignment = Alignment.Center,
    ) {
      Text(
          if (name == "MC") "M€" else name.take(2),
          style =
              palette.body.copy(fontSize = palette.body.fontSize * 0.7f, color = Color(0xFF2A2310)),
      )
    }
  }
}

/** Shows [text] in a small bubble below [content] while the pointer rests on it. */
@Composable
internal fun Tooltip(
    text: String?,
    modifier: Modifier = Modifier,
    maxLines: Int = 6,
    content: @Composable () -> Unit,
) {
  if (text.isNullOrBlank()) {
    Box(modifier) { content() }
    return
  }
  val palette = LocalPalette.current
  val source = remember { MutableInteractionSource() }
  val hovered by source.collectIsHoveredAsState()
  Box(modifier.hoverable(source)) {
    content()
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
                  ): IntOffset {
                    val below = anchorBounds.bottom + 8
                    val y =
                        if (below + popupContentSize.height <= windowSize.height) below
                        else anchorBounds.top - popupContentSize.height - 8
                    return IntOffset(
                        anchorBounds.left.coerceIn(
                            0,
                            (windowSize.width - popupContentSize.width).coerceAtLeast(0),
                        ),
                        y,
                    )
                  }
                }
              }
      ) {
        Text(
            text,
            Modifier.shadow(4.dp, RoundedCornerShape(4.dp))
                .background(palette.raised, RoundedCornerShape(4.dp))
                .border(palette.borderWidth, palette.line, RoundedCornerShape(4.dp))
                .widthIn(max = 380.dp)
                .padding(horizontal = 7.dp, vertical = 4.dp),
            style = palette.body.copy(fontSize = palette.body.fontSize * 0.86f),
            maxLines = maxLines,
        )
      }
    }
  }
}
