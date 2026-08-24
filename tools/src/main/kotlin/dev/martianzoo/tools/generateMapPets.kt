package dev.martianzoo.tools

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.Bundle
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.MarsMapDefinition
import java.nio.file.Files
import java.nio.file.Path

internal const val GENERATED_AREAS_COMMENT =
    "// The map area declarations below are code generated."

internal fun canonicalMapDeclarations(): List<ClassDeclaration> =
    Canon.bundles.flatMap { bundle ->
      val maps = orderedMaps(bundle)
      maps.map { it.asClassDeclaration } + maps.flatMap(::areaDeclarations)
    }

private fun orderedMaps(bundle: Bundle): List<MarsMapDefinition> {
  val bundleName = bundle.bundleName.toString()
  return bundle.marsMapDefinitions.sortedWith(
      compareBy<MarsMapDefinition> {
            val mapName = it.className.toString().removeSuffix("Map")
            bundleName.indexOf(mapName).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
          }
          .thenBy { it.className }
  )
}

private fun areaDeclarations(map: MarsMapDefinition): List<ClassDeclaration> =
    map.areas.sortedWith(compareBy({ it.row }, { it.column })).map { it.asClassDeclaration }

internal fun renderMapPets(maps: List<MarsMapDefinition>, alignedAreas: Boolean): String {
  require(maps.isNotEmpty()) { "Cannot render a bundle without maps" }
  val declarations = maps.map { it.asClassDeclaration } + maps.flatMap(::areaDeclarations)
  val source =
      if (alignedAreas) {
        buildString {
          append(maps.joinToString("\n", transform = ::renderAlignedMap))
          append("\n$GENERATED_AREAS_COMMENT\n")
          append(maps.joinToString("\n", transform = ::renderAlignedAreas))
        }
      } else {
        buildString {
          append(maps.joinToString("\n\n") { it.asClassDeclaration.toString() })
          append("\n\n$GENERATED_AREAS_COMMENT\n")
          append(
              maps.flatMap(::areaDeclarations).joinToString("\n") { it.toString(oneLine = true) }
          )
          append('\n')
        }
      }
  val parsed = parseClasses(source)
  check(parsed.map { it.className } == declarations.map { it.className }) {
    "Generated map Pets changed declaration identities"
  }
  check(parsed.take(maps.size) == declarations.take(maps.size)) {
    "Generated maps did not round-trip"
  }
  check(parsed.drop(maps.size) == declarations.drop(maps.size)) {
    "Generated map areas did not round-trip"
  }
  return source
}

private fun renderAlignedMap(map: MarsMapDefinition): String =
    map.asClassDeclaration.toString() + "\n"

private fun renderAlignedAreas(map: MarsMapDefinition): String = buildString {
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

internal fun canonicalMapPetsFiles(alignedAreas: Boolean): Map<String, String> = buildMap {
  Canon.bundles.forEach { bundle ->
    val maps = orderedMaps(bundle)
    if (maps.isNotEmpty()) {
      put(
          "canon/bundles/${bundle.bundleName}/map-classes.pets",
          renderMapPets(maps, alignedAreas),
      )
    }
  }
}

public fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: generateMapPets <output-directory> <aligned-areas>" }
  val output = Path.of(args[0])
  val alignedAreas = args[1].toBooleanStrict()
  canonicalMapPetsFiles(alignedAreas).forEach { (relativePath, source) ->
    val file = output.resolve(relativePath)
    Files.createDirectories(file.parent)
    Files.writeString(file, source)
  }
  println("Wrote ${canonicalMapDeclarations().size} declarations under $output")
}
