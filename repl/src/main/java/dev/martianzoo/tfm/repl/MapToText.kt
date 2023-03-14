package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.repl.TfmColor.CITY_TILE
import dev.martianzoo.tfm.repl.TfmColor.GREENERY_TILE
import dev.martianzoo.tfm.repl.TfmColor.OCEAN_TILE
import dev.martianzoo.tfm.repl.TfmColor.SPECIAL_TILE
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings

internal class MapToText(private val game: GameStateReader, val useColors: Boolean = true) {
  internal fun map(): List<String> {
    val cells = mutableListOf<SimpleCell>()
    cells +=
        (1..9).flatMap {
          listOf(
              fromRect(padCenter("$it —", 4), 3 * it, 0), // numerical row headings
              fromRect(padCenter("$it", 4), 0, it * 2 + 5), // numerical column headings
              fromRect(padCenter("/ ", 4), 1, it * 2 + 5), // slashes
          )
        }
    cells += game.setup.map.areas.map { fromHex(describe(it), it.row, it.column) }

    val grid = Grid.grid(cells, { it.row }, { it.column })
    return grid.rows().map { row -> row.joinToString("") { it?.s ?: "    " }.trimEnd() }
  }

  fun describe(area: AreaDefinition): String {
    val expression = expression("Tile<${area.className}>")
    val tile = game.getComponents(game.resolve(expression)).singleOrNull()
    return if (tile != null) {
      describe(tile)
    } else {
      describeEmpty(area)
    }
  }

  private fun describeEmpty(area: AreaDefinition): String {
    val color =
        when (area.kind) {
          cn("LandArea") -> TfmColor.LAND_AREA
          cn("WaterArea") -> TfmColor.WATER_AREA
          cn("VolcanicArea") -> TfmColor.LAND_AREA // TODO
          cn("NoctisArea") -> TfmColor.NOCTIS_AREA
          else -> error("")
        }
    return maybeColor(color, padCenter(area.code, 4))
  }

  fun maybeColor(c: TfmColor, s: String): String = if (useColors) c.foreground(s) else s

  fun fromHex(s: String, r: Int, c: Int) = fromRect(s, r * 3, c * 2 + 5 - r)
  fun fromRect(s: String, r: Int, c: Int) = SimpleCell(s, r, c)

  data class SimpleCell(val s: String, val row: Int, val column: Int) // TODO color?

  private fun describe(tile: Type): String {
    fun isIt(tile: Type, kind: String) = tile.isSubtypeOf(game.resolve(expression(kind)))

    val kind: Pair<String, TfmColor> =
        when { // TODO do this more by checking supertypes
          isIt(tile, "CityTile") -> "C" to CITY_TILE // keep this before S
          isIt(tile, "OceanTile") -> "O" to OCEAN_TILE
          isIt(tile, "GreeneryTile") -> "G" to GREENERY_TILE
          isIt(tile, "SpecialTile") -> "S" to SPECIAL_TILE
          else -> error("")
        }

    val argStrings = tile.expressionFull.arguments.toStrings()
    val player = argStrings.firstOrNull { it.startsWith("Player") }?.last() ?: ""

    return maybeColor(kind.second, padCenter("[${kind.first}$player]", 4))
  }
}

// TODO share
fun padCenter(s: String, length: Int): String {
  val before = (length - s.length + 1) / 2
  val after = (length - s.length) / 2
  return " ".repeat(before) + s + " ".repeat(after)
}
