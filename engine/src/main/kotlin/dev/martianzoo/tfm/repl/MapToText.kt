package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.Grid

class MapToText(val game: GameState) {

  internal fun map(): List<String> {
    val grid: Grid<MapAreaDefinition> = game.setup.authority.mapAreaDefinition("Tharsis") // DERP
    var indent: Int = grid.rowCount - 1
    var lines = ""
    // TODO

    var ind = "   ".repeat(grid.rowCount + 5)
    lines += "$ind 1     2     3     4     5     6     7     8     9\n"
    lines += "$ind/     /     /     /     /     /     /     /     /\n"

    val rows = grid.rows()
    for ((i, row) in rows.withIndex()) {
      if (i == 0) continue
      lines += "\n"
      lines += ("   ".repeat(indent--) + row.map { pad(describe(it)) }.joinToString("")).trimEnd() + "\n"
    }
    return lines.trimIndent()
        .split("\n")
        .mapIndexed { i, line ->
          val label = (i - 1) / 2
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
    if (tile.className.asString == "OceanTile") return "[O]"
    for (c: TypeExpression in (tile  as GenericTypeExpression).specs) {
      if (c.toString().startsWith("Player")) {
        val p = c.toString().substring(6)
        return when (tile.className.asString) {
          "CityTile" -> "[C$p]"
          "GreeneryTile" -> "[G$p]"
          "SpecialTile" -> "[S$p]"
          else -> error(tile)
        }
      }
    }
    error(tile)
  }

  private fun pad(s: String) = " ".repeat((7 - s.length) / 2) + s + " ".repeat((6 - s.length) / 2)
}
