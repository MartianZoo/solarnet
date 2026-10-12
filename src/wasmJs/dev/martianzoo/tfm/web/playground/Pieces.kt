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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.martianzoo.tfm.web.playground.resources.Res
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.decodeToImageBitmap

internal val LocalSpecimen = compositionLocalOf<Specimen> { error("no specimen") }

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

/** A player's (or Admin's) identity, shaded like a softly rounded game cube. */
@Composable
internal fun ActorSquare(actor: String?, size: Dp = 11.dp, modifier: Modifier = Modifier) {
  val color = actorColor(actor)
  val shape = RoundedCornerShape(size / 6)
  Box(modifier.size(size).clip(shape).background(color).border(0.5.dp, Color.Black, shape)) {
    Canvas(Modifier.fillMaxSize()) {
      val width = this.size.width
      val height = this.size.height
      val top = Path().apply {
        moveTo(0f, 0f)
        lineTo(width, 0f)
        lineTo(width * 0.72f, height * 0.28f)
        lineTo(width * 0.18f, height * 0.28f)
        close()
      }
      val side = Path().apply {
        moveTo(width, 0f)
        lineTo(width, height)
        lineTo(width * 0.72f, height)
        lineTo(width * 0.72f, height * 0.28f)
        close()
      }
      drawPath(top, Color.White.copy(alpha = 0.26f))
      drawPath(side, Color.Black.copy(alpha = 0.23f))
    }
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

private val imageCache = mutableMapOf<String, ImageBitmap?>()

/** Loads a bundled image (such as `files/cards/Trees.png`), or null while loading or if absent. */
@Composable
internal fun rememberBundledImage(path: String): ImageBitmap? {
  var image by remember(path) { mutableStateOf(imageCache[path]) }
  LaunchedEffect(path) {
    if (path !in imageCache) {
      imageCache[path] = runCatching { Res.readBytes(path).decodeToImageBitmap() }.getOrNull()
    }
    image = imageCache[path]
  }
  return image
}

/** A small game icon from the bundled icon set, with a lettered stand-in when it has none. */
@Composable
internal fun GameIcon(name: String, size: Dp, modifier: Modifier = Modifier) {
  val image = if (name == "MC") null else rememberBundledImage("files/icons/$name.png")
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
internal fun Tooltip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val palette = LocalPalette.current
  val source = remember { MutableInteractionSource() }
  val hovered by source.collectIsHoveredAsState()
  Box(modifier.hoverable(source)) {
    content()
    if (hovered) {
      Popup(offset = IntOffset(0, 22)) {
        Text(
            text,
            Modifier.shadow(4.dp, RoundedCornerShape(4.dp))
                .background(palette.raised, RoundedCornerShape(4.dp))
                .border(palette.borderWidth, palette.line, RoundedCornerShape(4.dp))
                .widthIn(max = 380.dp)
                .padding(horizontal = 7.dp, vertical = 4.dp),
            style = palette.body.copy(fontSize = palette.body.fontSize * 0.86f),
            maxLines = 3,
        )
      }
    }
  }
}
