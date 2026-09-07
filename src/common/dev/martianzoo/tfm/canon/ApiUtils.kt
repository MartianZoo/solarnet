package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.canon.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.canon.TfmClasses.PROD_OFFSET

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
    val standardResource = game.resolve(cn("StandardResource").expression)
    return standardResource.classTable
        .allSubclasses(standardResource.rootClass)
        .asSequence()
        .filterNot(Class::abstract)
        .map(Class::className)
        .toList()
        .toSetStrict()
  }

  /** Returns a map with six entries, giving [player]'s current printed production levels. */
  public fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith { resourceName ->
        val resource = resourceName.classExpression()
        val production = game.resolve(PRODUCTION.of(player, resource))
        val offset = game.resolve(PROD_OFFSET.of(player, resource))
        game.count(production) - game.count(offset)
      }

  /** Returns a map with six entries, giving [player]'s current printed production levels. */
  public fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /** Returns the mars map definition being used in this game (there must be exactly one). */
  public fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return game.tfmCatalog.marsMap(mapName)
  }
}
