package dev.martianzoo.tfm.web.playground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.martianzoo.tfm.web.playground.resources.Res
import dev.martianzoo.tfm.web.playground.resources.ibm_plex_mono_regular
import dev.martianzoo.tfm.web.playground.resources.ibm_plex_mono_semibold
import dev.martianzoo.tfm.web.playground.resources.ibm_plex_sans
import dev.martianzoo.tfm.web.playground.resources.inter
import dev.martianzoo.tfm.web.playground.resources.jetbrains_mono
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

private val playerColors =
    mapOf(
        "Violet" to Color(0xFF9A69E7),
        "Purple" to Color(0xFF9A69E7),
        "Yellow" to Color(0xFFFFE02F),
        "Black" to Color(0xFF565047),
        "Pink" to Color(0xFFFD97A7),
        "Blue" to Color(0xFF2389D0),
        "Orange" to Color(0xFFFFA10E),
        "Red" to Color(0xFFFA4630),
        "Green" to Color(0xFF29BF47),
        "Aqua" to Color(0xFF6CE1DB),
    )

/**
 * Font choices offered by the look-and-feel controls. Compose draws to a canvas, so it cannot use
 * the browser's generic families; every choice is a bundled face.
 */
internal enum class UiFont(val label: String, val monospace: Boolean) {
  IbmPlexSans("IBM Plex Sans", false),
  Inter("Inter", false),
  IbmPlexMono("IBM Plex Mono", true),
  JetBrainsMono("JetBrains Mono", true);

  @Composable
  fun family(): FontFamily =
      when (this) {
        IbmPlexSans -> variable(Res.font.ibm_plex_sans)
        Inter -> variable(Res.font.inter)
        IbmPlexMono ->
            FontFamily(
                Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
                Font(Res.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
            )
        JetBrainsMono -> variable(Res.font.jetbrains_mono)
      }

  companion object {
    fun named(name: String, fallback: UiFont): UiFont = entries.find { it.name == name } ?: fallback
  }
}

@Composable
private fun variable(resource: FontResource): FontFamily =
    FontFamily(
        Font(resource, FontWeight.Normal, variationSettings = weight(400)),
        Font(resource, FontWeight.Medium, variationSettings = weight(500)),
        Font(resource, FontWeight.SemiBold, variationSettings = weight(600)),
    )

private fun weight(value: Int) = FontVariation.Settings(FontVariation.weight(value))

/** [Style] resolved into drawing values. */
@Immutable
internal data class Palette(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val line: Color,
    val accent: Color,
    val mars: Color,
    val text: Color,
    val dimText: Color,
    val admin: Color,
    val borderWidth: Dp,
    val cornerRadius: Dp,
    val shadow: Dp,
    val spacing: Dp,
    val body: TextStyle,
    val mono: TextStyle,
) {
  /** The identity color of an actor; Admin and unknown actors use the pale neutral [admin]. */
  fun actor(name: String?): Color = playerColors[name] ?: admin

  val label: TextStyle
    get() = body.copy(fontSize = body.fontSize * 0.78f, letterSpacing = 0.9.sp, color = dimText)
}

internal val LocalPalette = staticCompositionLocalOf<Palette> { error("no palette") }

/** Keeps recorded player identities distinct even when their names are not color words. */
@Composable
internal fun actorColor(name: String?): Color {
  if (name == "SoloOpponent") return Color(0xFFE8E5DC)
  val palette = LocalPalette.current
  val names = LocalSpecimen.current.playerNames
  val seat = names.indexOf(name)
  return if (seat >= 0 && name !in playerColors) {
    val reserved = names.mapNotNull(playerColors::get).toSet()
    val available = playerColors.values.distinct().filterNot { it in reserved }
    val unnamedSeat = names.filterNot { it in playerColors }.indexOf(name)
    available[unnamedSeat % available.size]
  } else {
    palette.actor(name)
  }
}

@Composable
internal fun PlaygroundTheme(style: Style, content: @Composable () -> Unit) {
  val text = hexColor(style.text, Color.White)
  val palette =
      Palette(
          background = hexColor(style.background, Color.Black),
          surface = hexColor(style.surface, Color.Black),
          raised = hexColor(style.raised, Color.DarkGray),
          line = hexColor(style.line, Color.Gray),
          accent = hexColor(style.accent, Color.Cyan),
          mars = hexColor(style.mars, Color.Red),
          text = text,
          dimText = hexColor(style.dimText, Color.Gray),
          admin = hexColor(style.admin, Color.LightGray),
          borderWidth = style.borderWidth.dp,
          cornerRadius = style.cornerRadius.dp,
          shadow = style.shadow.dp,
          spacing = style.spacing.dp,
          body =
              TextStyle(
                  fontFamily = UiFont.named(style.font, UiFont.IbmPlexSans).family(),
                  fontSize = style.fontSize.sp,
                  color = text,
              ),
          mono =
              TextStyle(
                  fontFamily = UiFont.named(style.monoFont, UiFont.JetBrainsMono).family(),
                  fontSize = style.monoFontSize.sp,
                  color = text,
              ),
      )
  CompositionLocalProvider(LocalPalette provides palette, content = content)
}

/** Parses `#RRGGBB`, returning [fallback] for anything else. */
internal fun hexColor(text: String, fallback: Color): Color {
  val digits = text.trim().removePrefix("#")
  if (digits.length != 6) return fallback
  val rgb = digits.toLongOrNull(16) ?: return fallback
  return Color(0xFF000000 or rgb)
}
