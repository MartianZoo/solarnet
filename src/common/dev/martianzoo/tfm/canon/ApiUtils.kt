package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.canon.TfmClasses.PROD_OFFSET

/** Simple TfM-specific client helper functions, mostly for use by custom instructions. */
public object ApiUtils {
  /** Returns the name of every inhabited concrete `StandardResource` Class in [game]. */
  public fun standardResourceNames(game: GameReader): Set<ClassName> {
    val standardResource = game.resolve(cn("StandardResource").expression)
    return game.classTable
        .allSubclasses(standardResource.rootClass)
        .asSequence()
        .filterNot(Class::abstract)
        .map(Class::className)
        .toList()
        .toSetStrict()
  }

  /** Returns [player]'s current printed production level for each inhabited standard resource. */
  public fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith { resourceName ->
        val resource = resourceName.classExpression()
        val production = game.resolve(PRODUCTION.of(player, resource))
        val offset = game.resolve(PROD_OFFSET.of(player, resource))
        game.count(production) - game.count(offset)
      }

  /** Returns [player]'s current printed production level for each inhabited standard resource. */
  public fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /** Returns the mars map definition being used in this game (there must be exactly one). */
  public fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return game.tfmCatalog.marsMap(mapName)
  }
}
