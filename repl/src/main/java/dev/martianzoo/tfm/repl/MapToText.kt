package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings

internal class MapToText(private val game: ReadOnlyGameState) {
  internal fun map(): List<String> {
    val cells = mutableListOf<SimpleCell>()
    cells +=
        (1..9).flatMap {
          listOf(
              fromRect("$it —", 3 * it, 0), // numerical row headings
              fromRect("$it", 0, it * 2 + 5),  // numerical column headings
              fromRect("/ ", 1, it * 2 + 5),     // slashes
          )
        }
    cells += game.setup.map.areas.map { fromHex(describe(it), it.row, it.column) }

    val grid = Grid.grid(cells, { it.row }, { it.column })
    return grid.rows().map { row -> row.joinToString("") { padCenter(it?.s ?: "", 4) }.trimEnd() }
  }

  fun describe(area: AreaDefinition): String {
    val expression = expression("Tile<${area.className}>")
    val tiles = game.getComponents(game.resolve(expression))
    return tiles.singleOrNull()?.let { describe(it) } ?: area.code
  }

  fun fromHex(s: String, r: Int, c: Int) = fromRect(s, r * 3, c * 2 + 5 - r)
  fun fromRect(s: String, r: Int, c: Int) = SimpleCell(s, r, c)

  data class SimpleCell(val s: String, val row: Int, val column: Int) // TODO color?

  private fun describe(tile: Type): String {
    fun isIt(tile: Type, kind: String) = tile.isSubtypeOf(game.resolve(expression(kind)))

    val kind: String =
        when { // TODO do this more by checking supertypes
          isIt(tile, "CityTile") -> "C" // keep this before S
          isIt(tile, "OceanTile") -> "O"
          isIt(tile, "GreeneryTile") -> "G"
          isIt(tile, "SpecialTile") -> "S"
          else -> error("")
        }

    val argStrings = tile.expressionFull.arguments.toStrings()
    val player = argStrings.firstOrNull { it.startsWith("Player") }?.last() ?: ""

    return "[$kind$player]"
  }
}

// TODO share
fun padCenter(s: String, length: Int): String {
  val before = (length - s.length + 1) / 2
  val after = (length - s.length) / 2
  return " ".repeat(before) + s + " ".repeat(after)
}
