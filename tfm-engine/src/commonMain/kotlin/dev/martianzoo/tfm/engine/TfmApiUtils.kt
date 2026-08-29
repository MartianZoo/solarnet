package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.toSetStrict

/** Terraforming Mars helpers that depend only on the engine and canonical class vocabulary. */
public object TfmApiUtils {
  internal val mc: ClassName = cn("MC")

  internal val standardResourceClasses: Set<ClassName> =
      setOf(mc, cn("Steel"), cn("Titanium"), cn("Plant"), cn("Energy"), cn("Heat"))

  /** Returns the direct owner dependency of a concrete component type. */
  public fun getOwner(game: GameReader, component: Type): Type {
    val ownerType = game.resolve(OWNER.expression)
    val owner: Expression =
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
    val standardResource = game.resolve(cn("StandardResource").classExpression())
    val names =
        game
            .getComponents(standardResource)
            .map { it.expression.arguments.single().className }
            .toSet()
    return game.catalog.allClassNames.filter { it in names }.toSetStrict()
  }
}
