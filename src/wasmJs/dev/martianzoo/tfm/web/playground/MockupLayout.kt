package dev.martianzoo.tfm.web.playground

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The rectangular workspace regions. Each is a movable, resizable window in the mockup. */
internal enum class Region {
  Title,
  OutOfGame,
  LeftPane,
  RightPane,
  GlobalDashboard,
  MessageBar,
  TaskQueue,
  EventLog,
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
 * Everything the mockup lets the user rearrange or restyle. The development server persists it as
 * JSON; fields missing from a hand-edited file take their defaults.
 */
@Serializable
internal data class MockupLayout(
    val canvas: CanvasSize = CanvasSize(1600f, 1408f),
    val windows: Map<Region, Bounds> = DEFAULT_WINDOWS,
    /** Column widths in logical units, keyed by table. Each table's last column takes the rest. */
    val columns: Map<String, List<Float>> = DEFAULT_COLUMNS,
    val style: Style = Style(),
) {
  operator fun get(region: Region): Bounds = windows[region] ?: DEFAULT_WINDOWS.getValue(region)

  fun with(region: Region, bounds: Bounds): MockupLayout =
      copy(windows = windows + (region to bounds))

  fun columns(table: String): List<Float> =
      columns[table]?.takeIf { it.size == DEFAULT_COLUMNS.getValue(table).size }
          ?: DEFAULT_COLUMNS.getValue(table)

  fun withColumns(table: String, widths: List<Float>): MockupLayout =
      copy(columns = columns + (table to widths))

  fun toJson(): String = json.encodeToString(serializer(), this)

  companion object {
    const val TASK_TABLE: String = "taskQueue"
    const val EVENT_TABLE: String = "eventLog"

    private val json = Json {
      prettyPrint = true
      ignoreUnknownKeys = true
      encodeDefaults = true
    }

    fun fromJson(text: String): MockupLayout {
      val read = json.decodeFromString(serializer(), text)
      return read.copy(windows = DEFAULT_WINDOWS + read.windows)
    }

    private val DEFAULT_WINDOWS: Map<Region, Bounds> =
        mapOf(
            Region.Title to Bounds(16f, 12f, 760f, 52f),
            Region.OutOfGame to Bounds(1336f, 12f, 248f, 52f),
            Region.LeftPane to Bounds(16f, 76f, 716f, 640f),
            Region.RightPane to Bounds(744f, 76f, 716f, 640f),
            Region.GlobalDashboard to Bounds(1472f, 110f, 112f, 606f),
            Region.MessageBar to Bounds(16f, 728f, 1568f, 40f),
            Region.TaskQueue to Bounds(16f, 780f, 812f, 252f),
            Region.EventLog to Bounds(840f, 780f, 744f, 252f),
            Region.PreviousPlayer to Bounds(16f, 1044f, 36f, 348f),
            Region.PlayerDashboard to Bounds(64f, 1044f, 1472f, 96f),
            Region.PlayedCards to Bounds(64f, 1152f, 1472f, 240f),
            Region.NextPlayer to Bounds(1548f, 1044f, 36f, 348f),
        )

    private val DEFAULT_COLUMNS: Map<String, List<Float>> =
        mapOf(
            // ID, WHO, instruction, GIVEN BY, BECAUSE
            TASK_TABLE to listOf(58f, 66f, 470f, 140f, 78f),
            // event, WHO, delta, component, GIVEN BY, BECAUSE
            EVENT_TABLE to listOf(52f, 34f, 32f, 300f, 226f, 92f),
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
    val text: String = "#DCE7F2",
    val dimText: String = "#7F97AE",
    val admin: String = "#C9CED4",
    val borderWidth: Float = 1f,
    val cornerRadius: Float = 6f,
    val shadow: Float = 6f,
    val spacing: Float = 10f,
    val font: String = UiFont.IbmPlexSans.name,
    val fontSize: Float = 14f,
    val monoFont: String = UiFont.JetBrainsMono.name,
    val monoFontSize: Float = 12.5f,
)
