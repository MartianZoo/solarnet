package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.SpecialClassNames.GAME
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.filterWithoutNulls

/** Has functions for setting up new games and stuff. */
public object Engine {
  public fun newGame(setup: GameSetup): Game {
    val loader = PClassLoader(setup.authority, autoLoadDependencies = true)
    loader.loadAll(setup.allDefinitions().classNames())

    for (seat in 1..setup.players) {
      loader.load(cn("Player$seat"))
    }

    loader.frozen = true
    val game = Game(setup, ComponentGraph(), loader)

    // make class types also be singletons TODO
    val singletons = classInstances(loader) + singletons(loader)

    // have MarsMap take care of this via custom instruction TODO
    val borders = borders(setup.map, loader)

    val gameComponent = Component(loader.getClass(GAME).baseType)
    game.components.applyChange(gaining = gameComponent, hidden = true)
    require(game.changeLogFull().size == 1)

    // TODO make creating Game do this automatically??
    val cause = Cause(GAME.type, 0)
    for (it in singletons + borders) {
      game.components.applyChange(gaining = Component(it), cause = cause, hidden = true)
    }
    return game
  }

  // TODO maybe the loader should report these

  private fun classInstances(loader: PClassLoader): List<PType> =
      loader.allClasses.filter { !it.abstract }.map { it.classType }

  private fun singletons(loader: PClassLoader): List<PType> =
      // GAME *is* a singleton, but we already added it
      loader.allClasses
          .filter { it.isSingleton() && !it.baseType.abstract && it.className != GAME }
          .map { it.baseType }

  private fun borders(map: MarsMapDefinition, loader: PClassLoader): List<PType> {
    val border = cn("Border")
    return map.areas
        .let { it.rows() + it.columns() + it.diagonals() }
        .flatMap { it.windowed(2).filterWithoutNulls() }
        .flatMap { (one, two) ->
          val type1 = one.className.type
          val type2 = two.className.type
          listOf(
              border.addArgs(type1, type2),
              border.addArgs(type2, type1),
          )
        }
        .map { loader.resolveType(it) }
  }
}