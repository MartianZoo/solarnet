package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition

internal val milestonesAwardsExpansionBundle: StandardFormBundle by lazy {
  StandardFormBundle(
      "MilestonesAwardsExpansion",
      setOf(MilestonesAwardsExpansion.TileInLargestGroup),
  )
}

private object MilestonesAwardsExpansion {
  object TileInLargestGroup : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val player = getPlayerOwner(game, type)
      val areas = mapDefinition(game).areas
      val areasByName = areas.associateBy { it.className }
      val ownedTileClass = game.resolve(cn("OwnedTile").expression).rootClass
      val components = game.getComponents(game.resolve(cn("Component").expression))
      val ownedAreas =
          components
              .filter { component ->
                component.rootClass.isSubtypeOf(ownedTileClass) &&
                    getPlayerOwner(game, component) == player
              }
              .mapNotNullTo(linkedSetOf()) { tile ->
                tile.expressionFull.arguments.firstNotNullOfOrNull { argument ->
                  areasByName[argument.className]
                }
              }
      return areas.largestContiguousGroupSize(ownedAreas, { it.row }, { it.column })
    }
  }
}
