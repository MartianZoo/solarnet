package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialComponent.Production
import dev.martianzoo.tfm.pets.SpecialComponent.StandardResource
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpression): Map<ClassName, Int> {
  return game.getAll(StandardResource.classLiteral)
      .map { resourceClass ->
        val resourceName = resourceClass.className
        val sadAdjustment = if (resourceName == ClassName("Megacredit")) 5 else 0
        resourceName to game.count("$Production<$player, $resourceName.CLASS>") - sadAdjustment
      }.toMap()
}

fun standardResourceNames(game: ReadOnlyGameState): Set<ClassName> =
    game.getAll(StandardResource.classLiteral).map { it.className }.toSet()
