package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpr

// Note: this was easier to test in .engine than anywhere near here (ApiHelpersTest)
/**
 * Returns a map with six entries, giving [player]'s current production levels, adjusting megacredit
 * product to account for our horrible hack.
 */
fun lookUpProductionLevels(game: ReadOnlyGameState, player: TypeExpr): Map<ClassName, Int> =
    standardResourceNames(game).associateWith {
      val rawCount = game.countComponents(PRODUCTION.addArgs(player, CLASS.addArgs(it)))
      if (it == MEGACREDIT) {
        rawCount - 5
      } else {
        rawCount
      }
    }

/** Returns the name of every concrete class of type `StandardResource`. */
fun standardResourceNames(game: ReadOnlyGameState): Set<ClassName> =
    game.getComponents(CLASS.addArgs(STANDARD_RESOURCE))
        .map { it.arguments.single().className }
        .toSet()
