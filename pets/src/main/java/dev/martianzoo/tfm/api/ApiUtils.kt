package dev.martianzoo.tfm.api

import dev.martianzoo.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.util.toSetStrict

/** Simple TfM-specific client helper functions, mostly for use by custom instructions. */
object ApiUtils {
  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting
   * megacredit production to account for our GrossHack.
   */
  fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith {
        val type = game.resolve(PRODUCTION.of(player, it.classExpression()))
        game.count(type) - if (it == MEGACREDIT) 5 else 0
      }

  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting
   * megacredit production to account for our GrossHack.
   */
  fun lookUpProductionLevels(game: GameReader, player: Player) =
      lookUpProductionLevels(game, player.expression)

  /** Returns the name of every concrete class of type `StandardResource`. */
  fun standardResourceNames(game: GameReader): Set<ClassName> {
    val names =
        game
            .getComponents(game.resolve(STANDARD_RESOURCE.classExpression()))
            .map { it.expression.arguments.single().className }
            .toSet()
    // Put them in declaration order
    return game.authority.allClassNames.filter { it in names }.toSetStrict()
  }

  /** Returns the mars map definition being used in this game (there must be exactly one). */
  fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return (game.authority as TfmAuthority).marsMap(mapName)
  }
}
