package dev.martianzoo.tfm.api

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.types.Type
import dev.martianzoo.util.toSetStrict

/** Simple TfM-specific client helper functions, mostly for use by custom instructions. */
public object ApiUtils {
  /** Returns the direct owner dependency of a concrete component type. */
  public fun getOwner(game: GameReader, component: Type): Type {
    val ownerType: Type = game.resolve(OWNER.expression)
    val owner: Expression =
        component.expressionFull.arguments.single { game.resolve(it).narrows(ownerType, game) }
    return game.resolve(owner)
  }

  /** Returns [getOwner], requiring that the component is owned by a seated [Player]. */
  public fun getPlayerOwner(game: GameReader, component: Type): Player =
      Player.fromType(getOwner(game, component))
          ?: error("component is not owned by a Player: $component")

  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting
   * megacredit production to account for our GrossHack.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith {
        val type = game.resolve(PRODUCTION.of(player, it.classExpression()))
        game.count(type) - if (it == MEGACREDIT) 5 else 0
      }

  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting
   * megacredit production to account for our GrossHack.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /** Returns the name of every concrete class of type `StandardResource`. */
  public fun standardResourceNames(game: GameReader): Set<ClassName> {
    val names =
        game
            .getComponents(game.resolve(STANDARD_RESOURCE.classExpression()))
            .map { it.expression.arguments.single().className }
            .toSet()
    // Put them in declaration order
    return game.authority.allClassNames.filter { it in names }.toSetStrict()
  }

  /** Returns the mars map definition being used in this game (there must be exactly one). */
  public fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return game.tfmAuthority.marsMap(mapName)
  }
}
