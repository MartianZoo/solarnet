package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.ReadOnlyGameState
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings

internal class MapToText(private val game: ReadOnlyGameState) {

  internal fun map(): List<String> {
    val grid: Grid<AreaDefinition> = game.map.areas
    var lines = ""

    val ind = "   ".repeat(grid.rowCount + 5) // TODO why 5?
    lines += "$ind 1     2     3     4     5     6     7     8     9\n"
    lines += "$ind/     /     /     /     /     /     /     /     /\n"

    lines +=
        grid
            .rows()
            .mapIndexed { rowNum, row ->
              if (rowNum == 0) {
                ""
              } else {
                val rowString = row.joinToString("") { padCenter(describe(it), 6) }
                val indent = "   ".repeat(grid.rowCount - rowNum)
                "\n" + (indent + rowString).trimEnd() + "\n"
              }
            }
            .joinToString("")

    return lines.trimIndent().split("\n").mapIndexed { i, line ->
      val label = (i - 1) / 2 // TODO wow ugly
      if (i <= 2 || line.isEmpty()) {
        line
      } else {
        "$label —    $line"
      }
    }
  }

  private fun describe(area: AreaDefinition?): String { // TODO rewrite using Grid<String>
    if (area == null) return ""
    val typeExpr = typeExpr("Tile<${area.asClassDeclaration.name}>")
    val tiles = game.getComponents(game.resolveType(typeExpr))
    return when (tiles.size) {
      0 -> area.code
      1 -> describe(tiles.iterator().next())
      else -> error(tiles)
    }
  }

  private fun describe(tile: Type): String {
    val name = tile.typeExpr.className.toString()
    val kind =
        when { // TODO do this more by checking supertypes
          name == "Tile008" -> "C"
          name.startsWith("Tile") -> "S"
          else -> name[0]
        }
    val argStrings = tile.typeExprFull.arguments.toStrings()
    val player = argStrings.firstOrNull { it.startsWith("Player") }?.last() ?: ""
    return "[$kind$player]"
  }

  private fun padCenter(s: String, length: Int): String {
    val before = (length - s.length + 1) / 2
    val after = (length - s.length) / 2
    return " ".repeat(before) + s + " ".repeat(after)
  }
}
