package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialComponent.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialComponent.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpression): Map<ClassName, Int> {
  return game.getAll(STANDARD_RESOURCE.literal)
      .map { resourceClass ->
        val resourceName = resourceClass.className
        val sadAdjustment = if (resourceName == ClassName("Megacredit")) 5 else 0
        resourceName to game.count("$PRODUCTION<$player, $resourceName.CLASS>") - sadAdjustment
      }.toMap()
}

fun standardResourceNames(game: ReadOnlyGameState): Set<ClassName> =
    game.getAll(STANDARD_RESOURCE.literal).map { it.className }.toSet()
