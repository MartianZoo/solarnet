package dev.martianzoo.agenttestsupport

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.soloTileAreas
import dev.martianzoo.tfm.canon.soloTileSetup

/** Supplies the fixture's fixed solo locations before the World is created. */
internal fun withTestSoloSetup(
    premise: GamePremise,
    locations: List<ClassName> = emptyList(),
): GamePremise {
  if (cn("SoloMode") !in premise.modules) return premise
  if (
      locations.isEmpty() &&
          premise.premiseClassDeclarations.any { it.className == cn("SoloTileLocations") }
  )
      return premise
  val map =
      (premise.catalog as TfmCatalog).marsMapDefinitions.single { it.className in premise.modules }
  // The four most common printed project-card costs in Canon, in frequency order.
  val chosen = locations.ifEmpty { soloTileAreas(map, listOf(11, 10, 12, 8)).map { it.className } }
  return soloTileSetup(premise, chosen)
}
