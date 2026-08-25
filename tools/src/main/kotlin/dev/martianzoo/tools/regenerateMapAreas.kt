package dev.martianzoo.tools

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal fun regenerateMapAreas(source: String): String {
  val maps = MarsMapReader.readMaps(source)
  var result = source
  var nextHeader = 0
  maps.forEach { map ->
    val prefix = map.className.toString().removeSuffix("Map")
    val header = result.indexOf(MarsMapReader.GENERATED_AREAS_COMMENT, nextHeader)
    require(header >= 0) { "Missing generated-area comment for $prefix" }
    val diagramStart = result.indexOf('\n', header) + 1
    require(diagramStart > 0) { "Incomplete generated-area comment for $prefix" }
    var diagramEnd = diagramStart
    while (diagramEnd < result.length) {
      val lineEnd = result.indexOf('\n', diagramEnd).takeIf { it >= 0 } ?: result.length
      if (!result.substring(diagramEnd, lineEnd).trimStart().startsWith("//")) break
      diagramEnd = minOf(lineEnd + 1, result.length)
    }
    result = result.replaceRange(diagramStart, diagramEnd, renderDiagram(map) + '\n')

    val block = Regex("(?m)(?:^CLASS ${Regex.escape(prefix)}_\\d+_\\d+[^\\n]*(?:\\n|$)|^\\s*$\\n)*")
    val firstArea =
        Regex("(?m)^CLASS ${Regex.escape(prefix)}_\\d+_\\d+").find(result, header)
            ?: error("No generated area block for $prefix")
    val match =
        block.find(result, firstArea.range.first) ?: error("Cannot find area block for $prefix")
    check(match.range.first == firstArea.range.first)
    result = result.replaceRange(match.range, renderAreas(map).trimEnd() + "\n\n")
    nextHeader = header + MarsMapReader.GENERATED_AREAS_COMMENT.length
  }
  check(
      MarsMapReader.readMaps(result).flatMap { it.areas }.map { it.asClassDeclaration } ==
          parseClasses(result).filter { declaration ->
            maps.any { map ->
              declaration.className
                  .toString()
                  .startsWith(map.className.toString().removeSuffix("Map") + "_")
            }
          }
  ) {
    "Regenerated map areas did not round-trip"
  }
  return result.trimEnd() + '\n'
}

internal fun renderDiagram(map: MarsMapDefinition): String {
  val rows =
      map.areas.groupBy { it.row }.toSortedMap().values.map { row -> row.sortedBy { it.column } }
  val widestRow = rows.maxOf { it.size }
  return rows.joinToString("\n//\n") { row ->
    val leftmostCenter = DIAGRAM_PADDING + ((widestRow - row.size) * CELL_WIDTH + 1) / 2
    buildString {
      append("// ")
      row.forEachIndexed { index, area ->
        val start = leftmostCenter + index * CELL_WIDTH - (area.code.length - 1) / 2
        while (length < COMMENT_PREFIX_WIDTH + start) append(' ')
        append(area.code)
      }
    }
  }
}

private fun renderAreas(map: MarsMapDefinition): String = buildString {
  map.areas
      .groupBy { it.row }
      .toSortedMap()
      .values
      .forEachIndexed { rowIndex, row ->
        if (rowIndex > 0) append('\n')
        row.sortedBy { it.column }
            .forEach { area ->
              append("CLASS ${area.className} : ${area.kind.toString().padStart(12)}")
              append(" { row = ${area.row}; column = ${area.column}")
              area.bonusText?.let { append("; Tile<This>: $it") }
              append(" }\n")
            }
      }
}

private const val CELL_WIDTH = 6
private const val DIAGRAM_PADDING = 2
private const val COMMENT_PREFIX_WIDTH = 3

public fun main(args: Array<String>) {
  require(args.size == 1) { "Usage: regenerateMapAreas <canon-resources-directory>" }
  val root = Path.of(args.single())
  var changed = 0
  Files.walk(root).use { paths ->
    paths
        .filter { it.extension == "pets" }
        .sorted()
        .forEach { file ->
          val before = file.readText()
          if (MarsMapReader.GENERATED_AREAS_COMMENT in before) {
            val after = regenerateMapAreas(before)
            if (after != before) {
              file.writeText(after)
              changed++
            }
          }
        }
  }
  println("Regenerated map areas in $changed files")
}
