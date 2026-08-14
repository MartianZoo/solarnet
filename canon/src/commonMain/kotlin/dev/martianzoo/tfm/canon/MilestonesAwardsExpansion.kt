package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.api.ApiUtils.mapDefinition
import dev.martianzoo.types.Type

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
      val ownedTileClass = type.classTable.getClass(cn("OwnedTile"))
      val components = game.getComponents(game.resolve(cn("Component").expression))
      val ownedAreas =
          components
              .filter { component ->
                component.rootClass.isSubtypeOf(ownedTileClass) &&
                    getPlayerOwner(game, component) == player
              }
              .mapTo(linkedSetOf()) { tile ->
                val areaName =
                    tile.expressionFull.arguments
                        .single { argument -> argument.className in areasByName }
                        .className
                areasByName.getValue(areaName)
              }
      return areas.largestContiguousGroupSize(ownedAreas, { it.row }, { it.column })
    }
  }
}
