package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.data.TfmClasses
import dev.martianzoo.tfm.repl.TfmColor
import dev.martianzoo.tfm.repl.TfmColor.CITY_TILE
import dev.martianzoo.tfm.repl.TfmColor.GREENERY_TILE
import dev.martianzoo.tfm.repl.TfmColor.OCEAN_TILE
import dev.martianzoo.tfm.repl.TfmColor.SPECIAL_TILE
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings

internal class MapCommand(val repl: ReplSession) : ReplCommand("map") {
  override val usage = "map"
  override val help = """
        I mean it shows a map.
      """
  override val isReadOnly = true
  override fun noArgs() = MapToText(repl.tfm.reader, repl.jline != null).map()

  internal class MapToText(private val game: GameReader, val useColors: Boolean = true) {
    // my terminal app tries to show characters with H:W of 11:5
    // for a near-perfect hex grid you want 13:15
    // divide and you get 33:13 and fortunately that's pretty close to 5:2 (8:3 would be yikes)
    val horizStretch: Int = 5
    val vertStretch: Int = 2

    internal fun map(): List<String> {
      val grid: Grid<AreaDefinition> = ApiUtils.mapDefinition(game).areas

      val s = buildString {
        val clb = CenteringAppender(this)
        var countdown = grid.rowCount + 2 // not really sure why this works

        clb.appendHalfSpaces(countdown * horizStretch)
        repeat(grid.columnCount - 1) { clb.appendCentered(horizStretch, "  ${it + 1}") }
        clb.nl()

        clb.appendHalfSpaces(countdown-- * horizStretch)
        repeat(grid.columnCount - 1) { clb.appendCentered(horizStretch, "/") }

        countdown -= 2
        grid.rows().drop(1).forEach { row ->
          repeat(vertStretch) { clb.nl() }
          clb.appendHalfSpaces(countdown-- * horizStretch)
          for (area: AreaDefinition? in row) {
            val (text, color) = describe(area)
            clb.appendCentered(horizStretch, maybeColor(color, text), text.length)
          }
        }
        clb.nl()
      }
      val lines = s.trimIndent().split("\n")
      var display = 1
      return lines.mapIndexed { i, line ->
        val prefix =
            if ((i - vertStretch) % vertStretch == 1) {
              "${display++} -  "
            } else {
              ""
            }
        (prefix.padStart(6) + line).trimEnd()
      }
    }

    class CenteringAppender(val sb: StringBuilder) {
      var weird: Boolean = false
      fun append(s: String) = sb.append(s)!!
      fun appendHalfSpaces(n: Int) {
        append(" ".repeat(n / 2))
        if (n % 2 != 0) appendHalfSpace()
      }

      fun appendHalfSpace() {
        if (weird) append(" ")
        weird = !weird
      }

      fun appendCentered(width: Int, s: String, stringLength: Int = s.length) {
        val pad = width - stringLength
        require(pad >= 0)
        appendHalfSpaces(pad)
        append(s)
        appendHalfSpaces(pad)
      }

      fun nl() {
        append("\n")
        weird = false
      }
    }

    fun describe(area: AreaDefinition?): Pair<String, TfmColor> {
      if (area == null) return "" to TfmColor.NONE
      val expression = TfmClasses.TILE.of(area.className)
      val tile = game.getComponents(game.resolve(expression)).singleOrNull()
      return tile?.let(::describe) ?: describeEmpty(area)
    }

    private fun describeEmpty(area: AreaDefinition): Pair<String, TfmColor> {
      val color =
          when (area.kind) {
            ClassName.cn("LandArea") -> TfmColor.LAND_AREA
            ClassName.cn("WaterArea") -> TfmColor.WATER_AREA
            ClassName.cn("VolcanicArea") -> TfmColor.VOLCANIC_AREA
            ClassName.cn("NoctisArea") -> TfmColor.NOCTIS_AREA
            else -> error("unrecognized: ${area.kind}")
          }
      return area.code to color
    }

    fun maybeColor(c: TfmColor, s: String): String = if (useColors) c.foreground(s) else s

    private fun describe(tile: Type): Pair<String, TfmColor> {
      fun isIt(tile: Type, kind: String) = tile.narrows(game.resolve(ClassName.cn(kind).expression), game)

      val kind: Pair<String, TfmColor> =
          when {
            isIt(tile, "CityTile") -> "C" to CITY_TILE // keep this before S
            isIt(tile, "OceanTile") -> "O" to OCEAN_TILE
            isIt(tile, "GreeneryTile") -> "G" to GREENERY_TILE
            isIt(tile, "SpecialTile") -> "S" to SPECIAL_TILE
            else -> error("unrecognized: $tile")
          }

      val argStrings = tile.expressionFull.arguments.toStrings()
      val player = argStrings.firstOrNull(Player::isValid)?.last() ?: ""

      return "[${kind.first}$player]" to kind.second
    }
  }

}
