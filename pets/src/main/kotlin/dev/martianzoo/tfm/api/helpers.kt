package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr

// Note: this was easier to test in .engine than anywhere near here (ApiHelpersTest)
fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpr): Map<ClassName, Int> {
  return standardResourceNames(game)
      .map { resourceName ->
        val rawCount = game.count(typeExpr("$PRODUCTION<$player, $resourceName.CLASS>"))
        val sadAdjustment = if (resourceName == MEGACREDIT) 5 else 0
        resourceName to rawCount - sadAdjustment
      }
      .toMap()
}

fun standardResourceNames(game: ReadOnlyGameState): Set<ClassName> =
    game.getAll(STANDARD_RESOURCE.literal).map { it.className }.toSet()
