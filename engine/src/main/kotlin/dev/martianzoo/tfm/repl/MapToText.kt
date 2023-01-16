package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.Grid

class MapToText(val game: GameState) {

  internal fun map(): List<String> {
    val grid: Grid<MapAreaDefinition> = game.setup.map.areas
    var lines = ""
    // TODO

    var ind = "   ".repeat(grid.rowCount + 5)
    lines += "$ind 1     2     3     4     5     6     7     8     9\n"
    lines += "$ind/     /     /     /     /     /     /     /     /\n"

    lines += grid.rows().mapIndexed { rowNum, row ->
      if (rowNum == 0) {
        ""
      } else {
        val rowString = row.map { padCenter(describe(it), 6) }.joinToString("")
        val indent = "   ".repeat(grid.rowCount - rowNum)
        "\n" + (indent + rowString).trimEnd() + "\n"
      }
    }.joinToString("")

    return lines.trimIndent()
        .split("\n")
        .mapIndexed { i, line ->
          val label = (i - 1) / 2 // TODO wow ugly
          if (i <= 2 || line.isEmpty()) {
            line
          } else {
            "$label —    $line"
          }
        }
  }

  private fun describe(area: MapAreaDefinition?): String { // TODO rewrite using Grid<String>
    if (area == null) return ""
    val tileType: TypeInfo = game.resolve("Tile<${area.asClassDeclaration.name}>")
    val tiles = game.getAll(tileType.toTypeExpressionFull())
    return when (tiles.size) {
      0 -> area.code
      1 -> describe(tiles.iterator().next())
      else -> error(tiles)
    }
  }

  private fun describe(tile: TypeExpression): String {
    val kind = tile.className.asString[0]
    val player = (tile as GenericTypeExpression)
        .specs
        .map { "$it" }
        .firstOrNull { it.startsWith("Player") }
        ?.last()
        ?: ""
    return "[$kind$player]"
  }

  private fun padCenter(s: String, length: Int): String {
    val before = (length - s.length + 1) / 2
    val after = (length - s.length) / 2
    return " ".repeat(before) + s + " ".repeat(after)
  }
}
