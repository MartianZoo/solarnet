package dev.martianzoo.tfm.state

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.MarsMapDefinition
import dev.martianzoo.tfm.canon.TfmClasses.MARS_MAP
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.canon.TfmClasses.PROD_OFFSET

/** Read-only Terraforming Mars projections evaluated against the supplied [GameReader]. */
public object ApiUtils {
  /**
   * Returns the names of the inhabited, nonabstract subclasses of `StandardResource` in [game]'s
   * class table. This describes resource kinds available in the configured game, including kinds
   * for which no player currently owns a resource component.
   */
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

  /**
   * Returns [player]'s current printed production for each [standardResourceNames] entry.
   * Production is the count of `Production<player, resource>` minus the count of
   * `ProdOffset<player, resource>`; absent components contribute zero. The returned map is a
   * snapshot of this query, not a live view of later game changes.
   */
  public fun lookUpProductionLevels(game: GameReader, player: Expression): Map<ClassName, Int> =
      standardResourceNames(game).associateWith { resourceName ->
        val resource = resourceName.classExpression()
        val production = game.resolve(PRODUCTION.of(player, resource))
        val offset = game.resolve(PROD_OFFSET.of(player, resource))
        game.count(production) - game.count(offset)
      }

  /** Player overload of [lookUpProductionLevels] using [Player.expression]. */
  public fun lookUpProductionLevels(game: GameReader, player: Player): Map<ClassName, Int> =
      lookUpProductionLevels(game, player.expression)

  /**
   * Returns the catalog definition for the sole live `MarsMap` component in [game]. Fails if the
   * world has no map, multiple maps, or a map absent from its [tfmCatalog].
   */
  public fun mapDefinition(game: GameReader): MarsMapDefinition {
    val map = game.resolve(MARS_MAP.expression)
    val mapName = game.getComponents(map).single().className
    return game.tfmCatalog.marsMap(mapName)
  }
}
