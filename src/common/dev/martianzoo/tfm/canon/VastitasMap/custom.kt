package dev.martianzoo.tfm.canon.vastitasmap

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader
import dev.martianzoo.tfm.canon.ApiUtils.getOwningPlayer
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition

internal val customClasses: Set<CustomMetric> =
    setOf(
        object : CustomMetric("TileInLargestGroup") {
          override fun count(game: GameReader, type: Type): Int {
            val player = getOwningPlayer(game, type)
            val areas = mapDefinition(game).areas
            val areasByName = areas.associateBy { it.className }
            val ownedTiles = game.getComponents(game.resolve(cn("OwnedTile").of(player.expression)))
            val ownedAreas =
                ownedTiles.mapNotNullTo(linkedSetOf()) { tile ->
                  tile.typeDependencies.firstNotNullOfOrNull { dependency ->
                    areasByName[dependency.boundType.className]
                  }
                }
            return areas.largestContiguousGroupSize(ownedAreas, { it.row }, { it.column })
          }
        }
    )
