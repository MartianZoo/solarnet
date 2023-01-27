package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpr

// Note: this was easier to test in .engine than anywhere near here (ApiHelpersTest)
fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpr): Map<ClassName, Int> =
    standardResourceNames(game).associateWith {
      val rawCount = game.count(PRODUCTION.addArgs(player, it.literal))
      if (it == MEGACREDIT) {
        rawCount - 5
      } else {
        rawCount
      }
    }

fun standardResourceNames(game: ReadOnlyGameState): Set<ClassName> =
    game.getAll(STANDARD_RESOURCE.literal).map { it.args.single().root }.toSet()
