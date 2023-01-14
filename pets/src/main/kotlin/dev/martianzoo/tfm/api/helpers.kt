package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression

fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpression): Map<ClassName, Int> {
  return standardResourceNames(game).map { resourceName ->
    val rawCount = game.count("$PRODUCTION<$player, $resourceName.CLASS>")
    val sadAdjustment = if (resourceName == MEGACREDIT) 5 else 0
    resourceName to rawCount - sadAdjustment
  }.toMap()
}

fun standardResourceNames(game: ReadOnlyGameState): Set<ClassName> =
    game.getAll(STANDARD_RESOURCE.literal).map { it.className }.toSet()
