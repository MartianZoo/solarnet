package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.toSetStrict

// Note: this was easier to test in .engine than anywhere near here (ApiHelpersTest)

/**
 * Simple helper functions relating to standard resources, mostly for use by custom instructions.
 */
object ResourceUtils { // TODO this doesn't belong here
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

  fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /** Returns the name of every concrete class of type `StandardResource`. */
  fun standardResourceNames(game: GameReader): Set<ClassName> {
    val names = game
        .getComponents(game.resolve(STANDARD_RESOURCE.classExpression()))
        .map { it.expression.arguments.single().className }
        .toSet()
    // Put them in declaration order
    return game.setup.authority.allClassNames.filter { it in names }.toSetStrict()
  }

  private val MEGACREDIT = ClassName.cn("Megacredit")
  private val PRODUCTION = ClassName.cn("Production")
  private val STANDARD_RESOURCE = ClassName.cn("StandardResource")
}
