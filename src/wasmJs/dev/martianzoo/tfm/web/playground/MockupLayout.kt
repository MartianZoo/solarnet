package dev.martianzoo.tfm.web.playground

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The rectangular workspace regions. Each is a movable, resizable window in the mockup. */
internal enum class Region {
  Title,
  OutOfGame,
  Mars,
  Details,
  GlobalDashboard,
  MessageBar,
  History,
  PreviousPlayer,
  PlayerDashboard,
  PlayedCards,
  NextPlayer,
}

/** A rectangle in logical workspace units, which scale uniformly with the browser width. */
@Serializable
internal data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float) {
  val right: Float
    get() = x + width

  val bottom: Float
    get() = y + height
}

@Serializable internal data class CanvasSize(val width: Float, val height: Float)

/**
 * The code-defined layout and style. The development server writes adjustments as a JSON snapshot
 * for review; the app never reads that snapshot back.
 */
@Serializable
internal data class MockupLayout(
    val canvas: CanvasSize = CanvasSize(1600f, 2200f),
    val windows: Map<Region, Bounds> = DEFAULT_WINDOWS,
    /** Shared widths: #, WHO, content, BAL (empty for tasks), GIVEN BY, WHY. */
    val columns: List<Float> = listOf(48f, 40f, 370f, 36f, 246f, 48f),
    /** Share of the log region above its draggable divider. */
    val eventLogFraction: Float = 0.75f,
    val style: Style = Style(),
) {
  operator fun get(region: Region): Bounds = windows[region] ?: DEFAULT_WINDOWS.getValue(region)

  fun with(region: Region, bounds: Bounds): MockupLayout =
      copy(windows = windows + (region to bounds))

  fun toJson(): String = json.encodeToString(serializer(), this)

  companion object {
    private val json = Json {
      prettyPrint = true
      encodeDefaults = true
    }

    private val DEFAULT_WINDOWS: Map<Region, Bounds> =
        mapOf(
            Region.Title to Bounds(16f, 12f, 760f, 52f),
            Region.OutOfGame to Bounds(1336f, 12f, 248f, 52f),
            Region.Mars to Bounds(138f, 76f, 610f, 472f),
            Region.Details to Bounds(16f, 560f, 732f, 268f),
            Region.GlobalDashboard to Bounds(16f, 76f, 110f, 472f),
            Region.MessageBar to Bounds(16f, 840f, 1568f, 24f),
            Region.History to Bounds(764f, 76f, 820f, 752f),
            Region.PreviousPlayer to Bounds(16f, 876f, 36f, 1308f),
            Region.PlayerDashboard to Bounds(64f, 876f, 1472f, 80f),
            Region.PlayedCards to Bounds(64f, 968f, 1472f, 1216f),
            Region.NextPlayer to Bounds(1548f, 876f, 36f, 1308f),
        )
  }
}

/** Live look-and-feel values. Colors are `#RRGGBB` strings so the JSON stays hand-editable. */
@Serializable
internal data class Style(
    val background: String = "#0A1422",
    val surface: String = "#0F2033",
    val raised: String = "#15304A",
    val line: String = "#2C5170",
    val accent: String = "#4CC3C9",
    val mars: String = "#C2683A",
    val text: String = "#F3F4F6",
    val dimText: String = "#BCC4CC",
    val admin: String = "#C9CED4",
    val borderWidth: Float = 1.25f,
    val cornerRadius: Float = 1f,
    val shadow: Float = 6f,
    val spacing: Float = 12f,
    val font: String = UiFont.Inter.name,
    val fontSize: Float = 14f,
    val monoFont: String = UiFont.JetBrainsMono.name,
    val monoFontSize: Float = 12.5f,
)
