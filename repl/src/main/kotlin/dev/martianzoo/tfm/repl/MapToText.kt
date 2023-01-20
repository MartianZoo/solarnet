package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings

class MapToText(val game: GameState) {

  internal fun map(): List<String> {
    val grid: Grid<AreaDefinition> = game.map.areas
    var lines = ""

    val ind = "   ".repeat(grid.rowCount + 5) // TODO why 5?
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

  private fun describe(area: AreaDefinition?): String { // TODO rewrite using Grid<String>
    if (area == null) return ""
    val tileType: TypeInfo = game.resolve("Tile<${area.asClassDeclaration.name}>")
    val tiles = game.getAll(tileType.toTypeExpression())
    return when (tiles.size) {
      0 -> area.code
      1 -> describe(tiles.iterator().next())
      else -> error(tiles)
    }
  }

  private fun describe(tile: TypeExpression): String {
    val gentile = tile.asGeneric()
    val kind = gentile.root.toString()[0]
    val player = gentile.args
        .toStrings()
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
