package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.api.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.api.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.api.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression

// Note: this was easier to test in .engine than anywhere near here (ApiHelpersTest)

/**
 * Simple helper functions relating to standard resources, mostly for use by custom instructions.
 */
object ResourceUtils {
  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting
   * megacredit product to account for our horrible hack.
   */
  fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith {
        val type = game.resolve(PRODUCTION.addArgs(player, it.classExpression()))
        val rawCount = game.count(type)
        if (it == MEGACREDIT) {
          rawCount - 5
        } else {
          rawCount
        }
      }

  /** Returns the name of every concrete class of type `StandardResource`. */
  fun standardResourceNames(game: GameReader): Set<ClassName> =
      game
          .getComponents(game.resolve(STANDARD_RESOURCE.classExpression()))
          .map { it.expression.arguments.single().className }
          .toSet()
}
