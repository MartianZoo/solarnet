package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.filterWithoutNulls

/** Has functions for setting up new games and stuff. */
public object Engine {
  public fun loadClasses(setup: GameSetup): PClassLoader {
    val loader = PClassLoader(setup.authority, autoLoadDependencies = true)

    loader.load(GAME)
    loader.loadAll(setup.allDefinitions().classNames()) // all cards etc.

    for (seat in 1..setup.players) {
      loader.load(cn("Player$seat"))
    }

    loader.frozen = true
    return loader
  }

  public fun newGame(setup: GameSetup): Game {
    val loader = loadClasses(setup)
    val game = Game(setup, loader)
    game.applyChangeAndPublish(gaining = loader.getClass(GAME).baseType, hidden = true)

    // TODO custom instruction @createAll
    val borders = borders(setup.map, loader)

    // TODO custom instruction @createSingletons
    val cause = Cause(GAME.expr, 0)

    for (ptype in singletons(loader.allClasses) + borders) {
      val depInstances = ptype.dependencies.dependencies.map { it.bound } // TODO
      for (cpt in depInstances + ptype) { // TODO not ironclad
        if (game.count(cpt) == 0) {
          game.applyChangeAndPublish(gaining = game.resolve(cpt), cause = cause, hidden = true)
          cpt.pclass.allSuperclasses
              .flatMap { it.classEffects }
              .map { it.effect }
              .filter { it.trigger == WhenGain }
              .forEach { println("Should do ${it.instruction} now...") }
        }
      }
    }
    return game
  }

  private fun singletons(all: Set<PClass>): List<PType> =
      all.filter { it.isSingleton() }.flatMap { it.concreteTypesThisClass() }

  private fun borders(map: MarsMapDefinition, loader: PClassLoader): List<PType> {
    val border = cn("Border")
    return map.areas
        .let { it.rows() + it.columns() + it.diagonals() }
        .flatMap { it.windowed(2).filterWithoutNulls() }
        .map { pair -> pair.map { it.className.expr }}
        .flatMap { (area1, area2) ->
          listOf(
              border.addArgs(area1, area2),
              border.addArgs(area2, area1),
          )
        }
        .map { loader.resolve(it) }
  }
}
