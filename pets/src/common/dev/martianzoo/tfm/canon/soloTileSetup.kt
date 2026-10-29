package dev.martianzoo.tfm.canon

import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Declares the fixed solo setup locations, ordered city, adjacent greenery, city, adjacent
 * greenery. Adds a selected premise-local declaration. Game files retain it, so replay uses the
 * same locations. Setup validates the locations; ordinary Pets instructions place the tiles.
 *
 * Setup chooses these locations before creating the World, for example using [soloTileAreas]. This
 * function performs no draw or random choice. Requires four distinct available land areas on the
 * selected map, nonadjacent cities, and each greenery adjacent to its corresponding city. An
 * invalid configuration throws [IllegalArgumentException] without changing [premise]. Calling it
 * again replaces the earlier locations.
 */
public fun soloTileSetup(premise: GamePremise, locations: List<ClassName>): GamePremise {
  require(cn("SoloMode") in premise.modules) { "Solo tile setup requires SoloMode" }
  require(locations.size == 4 && locations.distinct().size == 4) {
    "Solo setup requires four distinct locations in city, greenery, city, greenery order"
  }
  val map =
      (premise.catalog as TfmCatalog).marsMapDefinitions.single { it.className in premise.modules }
  val areas = locations.map { name ->
    requireNotNull(map.areas.singleOrNull { it.className == name }) {
      "Solo location $name is not on ${map.className}"
    }
  }
  require(areas.none { it.kind == cn("WaterArea") || it.kind == cn("NoctisArea") }) {
    "Solo tiles require available land areas"
  }
  require(areas[2] !in map.areas.hexNeighbors(areas[0].row, areas[0].column)) {
    "Solo cities cannot be adjacent"
  }
  for ((city, greenery) in areas.chunked(2)) {
    require(greenery in map.areas.hexNeighbors(city.row, city.column)) {
      "Solo greenery must be adjacent to its city"
    }
  }
  val declaration =
      parseClasses("CLASS SoloTileLocations : PlaceNeutralTiles<${locations.joinToString()}>")
          .single()
  return premise.copy(
      classSelections = premise.classSelections + ClassSelection(declaration.className),
      premiseClassDeclarations =
          premise.premiseClassDeclarations
              .filterNot { it.className == declaration.className }
              .toSet() + declaration,
  )
}
