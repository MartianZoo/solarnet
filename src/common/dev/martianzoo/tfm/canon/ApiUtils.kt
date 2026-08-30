package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.canon.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.canon.TfmClasses.MC
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION

/** Simple TfM-specific client helper functions, mostly for use by custom instructions. */
public object ApiUtils {
  /** Returns the direct owner dependency of a concrete component type. */
  public fun getOwner(game: GameReader, component: Type): Type {
    val ownerType = game.resolve(OWNER.expression)
    val owner =
        component.expressionFull.arguments.single { game.resolve(it).narrows(ownerType, game) }
    return game.resolve(owner)
  }

  /** Returns [getOwner], requiring that the component is owned by a seated [Player]. */
  public fun getPlayerOwner(game: GameReader, component: Type): Player {
    val ownerName = getOwner(game, component).className
    return game.actors.filterIsInstance<Player>().singleOrNull { it.className == ownerName }
        ?: error("component is not owned by a Player: $component")
  }

  /** Returns the name of every concrete class of type `StandardResource`. */
  public fun standardResourceNames(game: GameReader): Set<ClassName> {
    val standardResource =
        game.resolve(ClassName.Companion.cn("StandardResource").classExpression())
    val names =
        game
            .getComponents(standardResource)
            .map { it.expression.arguments.single().className }
            .toSet()
    return game.catalog.allClassNames.filter { it in names }.toSetStrict()
  }

  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting mc
   * production to account for our GrossHack.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith {
        val type = game.resolve(PRODUCTION.of(player, it.classExpression()))
        game.count(type) - if (it == MC) 5 else 0
      }

  /**
   * Returns a map with six entries, giving [player]'s current production levels, adjusting mc
   * production to account for our GrossHack.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /** Returns the mars map definition being used in this game (there must be exactly one). */
  public fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return game.tfmCatalog.marsMap(mapName)
  }
}
