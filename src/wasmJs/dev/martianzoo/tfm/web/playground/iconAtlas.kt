package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import dev.martianzoo.tfm.web.playground.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.decodeToImageBitmap

private val iconScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
private val atlasLoad: Deferred<Pair<ImageBitmap, Map<String, IntRect>>> by lazy {
  iconScope.async { readAtlas() }
}

private suspend fun readAtlas(): Pair<ImageBitmap, Map<String, IntRect>> {
  var retryDelay = 1000L
  while (true) {
    try {
      val positions =
          Res.readBytes("files/icons/index.tsv")
              .decodeToString()
              .lineSequence()
              .drop(1)
              .filter(String::isNotBlank)
              .associate { line ->
                val (name, x, y, width, height) = line.split('\t')
                val left = x.toInt()
                val top = y.toInt()
                name to IntRect(left, top, left + width.toInt(), top + height.toInt())
              }
      return Res.readBytes("files/icons/atlas.png").decodeToImageBitmap() to positions
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      delay(retryDelay)
      retryDelay = minOf(retryDelay * 2, 60_000L)
    }
  }
}

/** Returns the named icon from the bundled atlas, or null while loading or when absent. */
@Composable
internal fun rememberClassIcon(name: String): Painter? {
  var painter by remember(name) { mutableStateOf<Painter?>(null) }
  LaunchedEffect(name) {
    val (image, positions) = atlasLoad.await()
    val bounds = positions[name] ?: return@LaunchedEffect
    painter =
        BitmapPainter(
            image,
            srcOffset = IntOffset(bounds.left, bounds.top),
            srcSize = IntSize(bounds.width, bounds.height),
        )
  }
  return painter
}
