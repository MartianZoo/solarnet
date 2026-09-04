package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.util.Grid
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition

/** Reads the compact map diagrams embedded in Pets source comments. */
public object MarsMapReader {
  public const val GENERATED_AREAS_COMMENT: String =
      "// The map areas below are code-generated based on the following comment"

  public fun readMaps(source: String): List<MarsMapDefinition> {
    val lines = source.lines()
    return lines.indices.mapNotNull { headerIndex ->
      if (lines[headerIndex].trim() != GENERATED_AREAS_COMMENT) return@mapNotNull null

      val section =
          lines.subList(
              headerIndex + 1,
              lines
                  .indexOfFirstAfter(headerIndex) { it.trim() == GENERATED_AREAS_COMMENT }
                  .takeIf { it >= 0 } ?: lines.size,
          )
      val diagramRows =
          section
              .takeWhile { it.trimStart().startsWith("//") }
              .map { it.substringAfter("//") }
              .filter { it.isNotBlank() }
              .mapIndexed { rowIndex, row -> diagramRow(rowIndex + 1, row) }
      require(diagramRows.isNotEmpty()) { "Map diagram has no rows" }

      val firstArea =
          section.firstNotNullOfOrNull(AREA_CLASS::matchEntire)
              ?: error("Map diagram is not followed by generated area classes")
      val prefix = firstArea.groupValues[1]
      val leftmostSlantPosition = diagramRows.minOf(DiagramRow::slantPosition)
      val areaCodes = diagramRows.flatMap { row ->
        val slantOffset = row.slantPosition - leftmostSlantPosition
        require(slantOffset % DIAGRAM_CELL_WIDTH == 0) {
          "Map row ${row.number} is not aligned to a slant-column"
        }
        val firstColumn = slantOffset / DIAGRAM_CELL_WIDTH + 1
        row.codes.mapIndexed { columnIndex, code ->
          AreaCode(row.number, firstColumn + columnIndex, code)
        }
      }
      val coordinateWidth = if (areaCodes.any { it.row >= 10 || it.column >= 10 }) 2 else 1
      val areas = areaCodes.map { area(prefix, it, coordinateWidth) }
      val mapName = cn("${prefix}Map")
      MarsMapDefinition(
          className = mapName,
          areas = Grid.grid(areas, AreaDefinition::row, AreaDefinition::column),
      )
    }
  }

  private fun diagramRow(number: Int, source: String): DiagramRow {
    val codes = Regex("\\S+").findAll(source).toList()
    require(codes.isNotEmpty()) { "Map diagram row $number has no areas" }
    val first = codes.first()
    val firstCenter = first.range.first + (first.value.length - 1) / 2
    return DiagramRow(
        number,
        firstCenter + (number - 1) * DIAGRAM_ROW_SLANT,
        codes.map { it.value },
    )
  }

  private fun area(prefix: String, areaCode: AreaCode, coordinateWidth: Int): AreaDefinition {
    val (row, column, code) = areaCode
    require(code.isNotEmpty()) { "Empty map-area code" }
    require(row < 100) { "Map row needs more than two digits: $row" }
    require(column < 100) { "Map column needs more than two digits: $column" }
    val kind =
        when (code.first()) {
          'L' -> "LandArea"
          'W' -> "WaterArea"
          'V' -> "VolcanicArea"
          'N' -> "NoctisArea"
          else -> error("Unknown map-area kind in `$code`")
        }
    val rowText = row.toString().padStart(coordinateWidth, '0')
    val columnText = column.toString().padStart(coordinateWidth, '0')
    return AreaDefinition(
        cn("${prefix}_${rowText}_$columnText"),
        row,
        column,
        cn(kind),
        bonus(code.drop(1)),
        code,
    )
  }

  private fun bonus(code: String): String? {
    return decodeBonusCodes(code)
        .filterNot { (_, symbol) -> BONUSES.getValue(symbol) == "Ok" }
        .map { (count, symbol) ->
          val bonus = BONUSES.getValue(symbol)
          if (count == 1) bonus else "$count $bonus"
        }
        .joinToString()
        .ifEmpty { null }
  }

  /** Expands multiplier prefixes while preserving a final digit as its own bonus sigil. */
  internal fun expandBonusCodes(code: String): List<Char> =
      decodeBonusCodes(code).flatMap { (count, symbol) -> List(count) { symbol } }

  private fun decodeBonusCodes(code: String): List<Pair<Int, Char>> {
    val symbols = ArrayDeque(code.toList())
    return buildList {
      while (symbols.isNotEmpty()) {
        val symbol = symbols.removeFirst()
        add(
            when {
              symbol in '2'..'9' && symbols.isNotEmpty() ->
                  symbol.digitToInt() to symbols.removeFirst()
              symbol == symbols.firstOrNull() -> 2 to symbols.removeFirst()
              else -> 1 to symbol
            }
        )
      }
    }
  }

  private val BONUSES =
      mapOf(
          'P' to "Plant",
          'S' to "Steel",
          'T' to "Titanium",
          'C' to "ProjectCard",
          'E' to "Energy",
          'H' to "Heat",
          'R' to "StandardResource",
          'D' to "Ok",
          'F' to "TemperatureStep",
          '4' to "-4 MC",
          '6' to "-6 MC",
          'X' to "CimmeriaPlacementBonus",
          'O' to "OceanTile<>",
      )
  private val AREA_CLASS = Regex("\\s*CLASS ([A-Za-z][A-Za-z0-9]*)_\\d+_\\d+.*")

  private data class DiagramRow(val number: Int, val slantPosition: Int, val codes: List<String>)

  private data class AreaCode(val row: Int, val column: Int, val code: String)

  private const val DIAGRAM_CELL_WIDTH = 6
  private const val DIAGRAM_ROW_SLANT = DIAGRAM_CELL_WIDTH / 2

  private inline fun <T> List<T>.indexOfFirstAfter(index: Int, predicate: (T) -> Boolean): Int {
    for (candidate in index + 1 until size) if (predicate(get(candidate))) return candidate
    return -1
  }
}
