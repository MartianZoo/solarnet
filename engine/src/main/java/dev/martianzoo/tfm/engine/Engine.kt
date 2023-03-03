package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.filterWithoutNulls

/** Has functions for setting up new games and stuff. */
public object Engine {
  public fun newGame(setup: GameSetup): Game {
    val loader = PClassLoader(setup.authority, autoLoadDependencies = true)

    for (seat in 1..setup.players) {
      loader.load(cn("Player$seat"))
    }
    loader.load(cn("Border")) // TODO

    val gameComponent = Component(loader.load(GAME))
    loader.loadAll(setup.allDefinitions().classNames())

    val game = Game(setup, loader)
    game.components.applyChange(gaining = gameComponent, hidden = true)

    // have MarsMap take care of this via custom instruction TODO
    val borders = borders(setup.map, loader)

    // TODO make creating Game do this automatically
    val cause = Cause(GAME.type, 0)
    for (it in singletons(loader) + borders) {
      val gaining = Component(it)
      for (cpt in gaining.dependencies + gaining) { // TODO not ironclad
        if (game.countComponents(cpt.type) == 0) {
          println(cpt)
          game.components.applyChange(gaining = cpt, cause = cause, hidden = true)
        }
      }
    }
    return game
  }

  private fun singletons(loader: PClassLoader): List<PType> =
      loader.allClasses.filter { it.isSingleton() }.flatMap { it.allConcreteTypes() }

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
