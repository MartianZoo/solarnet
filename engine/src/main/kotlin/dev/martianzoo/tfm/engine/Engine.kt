package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.PetClass
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.tfm.types.PetType.PetClassLiteral
import dev.martianzoo.util.filterNoNulls

object Engine {
  fun newGame(setup: GameSetup): Game {
    val loader = PetClassLoader(setup.authority)
    loader.autoLoadDependencies = true

    val classes = setup.authority
        .allDefinitions
        .filter { it.bundle in setup.bundles } // TODO auth
        .map { it.name }
        .sorted()

    loader.loadAll(classes)

    for (seat in 1..setup.players) {
      loader.load("Player$seat")
    }

    // Hacks TODO
    loader.load(ME)
    loader.load(PRODUCTION)
    loader.freeze()

    val prebuilt = classLiterals(loader) + // TODO make them just singletons too!?
        singletons(loader) +
        borders(setup.map, loader) // TODO

    return Game(setup, ComponentGraph(prebuilt), loader)
  }

  // TODO maybe the loader should report these

  private fun classLiterals(loader: PetClassLoader) =
      loader.loadedClasses()
          .filterNot { it.abstract }
          .map { Component(PetClassLiteral(it)) }

  private fun singletons(loader: PetClassLoader) =
      loader.loadedClasses()
          .filter { isSingleton(it) && !it.baseType.abstract } // TODO that's not right
          .map { Component(loader.resolve(it.name.type)) }

  // includes abstract
  fun isSingleton(pc: PetClass): Boolean =
      pc.invariantsRaw.any { it.requiresThis() } ||
      pc.directSuperclasses.any { isSingleton(it) }


  fun borders(map: MarsMapDefinition, loader: PetClassLoader): List<Component> {
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
        .map { Component(loader.resolve(it)) }
  }
}
