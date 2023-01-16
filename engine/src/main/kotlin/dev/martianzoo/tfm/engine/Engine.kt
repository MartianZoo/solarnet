package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.MapDefinition
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.util.filterNoNulls

object Engine {
  fun newGame(
      authority: Authority,
      playerCount: Int,
      bundles: Collection<String>,
  ): Game {
    return newGame(GameSetup(authority, playerCount, bundles))
  }

  fun newGame(setup: GameSetup): Game {
    val loader = PetClassLoader(setup.authority)
    loader.autoLoadDependencies = true

    for (seat in 1..setup.players) {
      loader.load("Player$seat")
    }

    val classes = setup.authority
        .allDefinitions
        .filter { it.bundle in setup.bundles } // TODO auth
        .map { it.name }
        .sorted()

    loader.loadAll(classes)

    // Hacks TODO
    loader.load(PRODUCTION)

    loader.freeze()

    val prebuilt = mutableListOf<Component>()

    prebuilt += loader.loadedClasses()
        .filterNot { it.abstract }
        .map { Component(it) } // creates a class literal

    prebuilt += loader.loadedClasses()
        .filter { it.isSingleton() && !it.baseType.abstract } // TODO that's not right
        .map { Component(loader.resolve(it.name.type)) }

    prebuilt += borders(setup.map).map { Component(loader.resolve(it)) }

    return Game(setup, ComponentGraph(prebuilt), loader)
  }

  fun borders(map: MapDefinition): List<TypeExpression> {
    val border = ClassName("Border")
    return map.areas
        .let { it.rows() + it.columns() + it.diagonals() }
        .flatMap { it.windowed(2).filterNoNulls() }
        .flatMap { (one, two) ->
          val type1 = one.name.type
          val type2 = two.name.type
          listOf(
              border.specialize(type1, type2),
              border.specialize(type2, type1),
          )
        }
  }
}
