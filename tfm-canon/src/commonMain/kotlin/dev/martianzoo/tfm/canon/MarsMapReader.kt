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
      val diagram =
          section
              .takeWhile { it.trimStart().startsWith("//") }
              .map { it.substringAfter("//").trim() }
              .filter { it.isNotEmpty() }
      require(diagram.isNotEmpty()) { "Map diagram has no rows" }

      val firstArea =
          section.firstNotNullOfOrNull(AREA_CLASS::matchEntire)
              ?: error("Map diagram is not followed by generated area classes")
      val prefix = firstArea.groupValues[1]
      val areas = diagram.flatMapIndexed { rowIndex, row ->
        val rowNumber = rowIndex + 1
        val firstColumn = maxOf(1, rowNumber - 4)
        row.split(Regex("\\s+")).mapIndexed { columnIndex, code ->
          area(prefix, rowNumber, firstColumn + columnIndex, code)
        }
      }
      val declarations =
          source
              .lineSequence()
              .mapNotNull { CLASS_NAME.matchEntire(it)?.groupValues?.get(1) }
              .map(::cn)
              .toSet()
      MarsMapDefinition(
          className = cn("${prefix}Map"),
          areas = Grid.grid(areas, AreaDefinition::row, AreaDefinition::column),
          defaultMilestones = cn("${prefix}Milestone").takeIf(declarations::contains),
          defaultAwards = cn("${prefix}Award").takeIf(declarations::contains),
      )
    }
  }

  private fun area(prefix: String, row: Int, column: Int, code: String): AreaDefinition {
    require(code.isNotEmpty()) { "Empty map-area code" }
    val kind =
        when (code.first()) {
          'L' -> "LandArea"
          'W' -> "WaterArea"
          'V' -> "VolcanicArea"
          'N' -> "NoctisArea"
          else -> error("Unknown map-area kind in `$code`")
        }
    return AreaDefinition(cn(prefix), row, column, cn(kind), bonus(code.drop(1)), code)
  }

  private fun bonus(code: String): String? {
    val symbols = ArrayDeque(code.toList())
    return generateSequence {
      if (symbols.isEmpty()) return@generateSequence null
      when (val symbol = symbols.removeFirst()) {
        in '2'..'9' -> "$symbol ${BONUSES.getValue(symbols.removeFirst())}"
        symbols.firstOrNull() -> "2 ${BONUSES.getValue(symbols.removeFirst())}"
        else -> BONUSES.getValue(symbol)
      }
    }
        .joinToString()
        .ifEmpty { null }
  }

  private val BONUSES =
      mapOf(
          'P' to "Plant",
          'S' to "Steel",
          'T' to "Titanium",
          'C' to "ProjectCard",
          'E' to "Energy",
          'H' to "Heat",
          'X' to "TcColonyBonus",
          'O' to "OceanTile<>",
          '-' to "-6 MC",
      )
  private val AREA_CLASS = Regex("\\s*CLASS ([A-Za-z][A-Za-z0-9]*)_\\d+_\\d+.*")
  private val CLASS_NAME = Regex("\\s*(?:ABSTRACT )?CLASS ([A-Za-z][A-Za-z0-9]*).*")

  private inline fun <T> List<T>.indexOfFirstAfter(index: Int, predicate: (T) -> Boolean): Int {
    for (candidate in index + 1 until size) if (predicate(get(candidate))) return candidate
    return -1
  }
}
