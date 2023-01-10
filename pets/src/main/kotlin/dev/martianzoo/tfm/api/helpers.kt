package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialComponent.Production
import dev.martianzoo.tfm.pets.SpecialComponent.StandardResource
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpression): Map<String, Int> {
  return game.getAll(StandardResource.classEx)
      .map { resourceClass ->
        val resourceName = resourceClass.className
        val sadAdjustment = if (resourceName == "Megacredit") 5 else 0
        resourceName to game.count("${Production.name}<$player, $resourceName.CLASS>") -
            sadAdjustment
      }.toMap()
}

fun standardResourceNames(game: ReadOnlyGameState): Set<String> =
    game.getAll(StandardResource.classEx).map { it.className }.toSet()
