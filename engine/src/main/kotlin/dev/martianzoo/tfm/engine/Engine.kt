package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.util.filterNoNulls

public object Engine {
  public fun newGame(setup: GameSetup): Game {
    val loader = PClassLoader(setup.authority, autoLoadDependencies = true)

    val defns = setup.allDefinitions.map { it.name }.sorted()
    loader.loadAll(defns)

    for (seat in 1..setup.players) {
      loader.load(cn("Player$seat"))
    }

    // Hacks TODO
    loader.load(PRODUCTION)
    loader.load(ME)
    loader.load(cn("MegacreditProductionHack")) // TODO loopy singletons
    loader.load(cn("MetalHandler")) // TODO uhhhh ?
    loader.frozen = true

    val prebuilt =
        classInstances(loader) + // TODO make them just singletons too!?
        singletons(loader) +
        borders(setup.map, loader) // TODO

    return Game(setup, ComponentGraph(prebuilt), loader)
  }

  // TODO maybe the loader should report these

  private fun classInstances(loader: PClassLoader): List<Component> {
    val concretes = loader.allClasses.filter { !it.abstract }
    return concretes.map { Component(it.toClassType()) }
  }

  private fun singletons(loader: PClassLoader) =
      loader.allClasses
          .filter { it.isSingleton() && !it.baseType.abstract } // TODO that's not right
          .map { Component(it.baseType) }

  private fun borders(map: MarsMapDefinition, loader: PClassLoader): List<Component> {
    val border = cn("Border")
    return map.areas
        .let { it.rows() + it.columns() + it.diagonals() }
        .flatMap { it.windowed(2).filterNoNulls() }
        .flatMap { (one, two) ->
          val type1 = one.name.type
          val type2 = two.name.type
          listOf(
              border.addArgs(type1, type2),
              border.addArgs(type2, type1),
          )
        }
        .map { Component(loader.resolveType(it)) }
  }
}
