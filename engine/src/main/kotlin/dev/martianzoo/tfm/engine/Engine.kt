package dev.martianzoo.tfm.engine

import com.google.common.collect.HashMultiset
import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.util.filterNoNulls

object Engine {
  internal fun newGame(
      authority: Authority,
      playerCount: Int,
      bundles: Collection<String>,
  ): Game {
    return newGame(GameSetup(authority, playerCount, bundles))
  }

  internal fun newGame(setup: GameSetup): Game {
    val loader = PetClassLoader(setup.authority)
    loader.autoLoadDependencies = true

    for (seat in 1..setup.players) {
      loader.load("Player$seat")
    }

    val cards = setup.authority.allDefinitions.filter { it.bundle in setup.bundles } // TODO auth
    loader.loadAll(cards.map { it.name }.sorted())

    // Hacks TODO
    loader.load(PRODUCTION)
    loader.load("Border")

    loader.freeze()

    val prebuilt = HashMultiset.create<Component>()
    loader.loadedClasses().forEach {
      if (!it.abstract) {
        prebuilt.add(Component(it))
      }
      if (it.isSingleton() && !it.baseType.abstract) {
        prebuilt.add(Component(loader.resolve(gte(it.name))))
      }
    }
    if (setup.grid != null) {
      prebuilt.addAll(borders(setup).map { Component(loader.resolve(it)) })
    }

    return Game(setup, ComponentGraph(prebuilt), loader)
  }

  fun borders(setup: GameSetup): List<TypeExpression> {
    val border = ClassName("Border")
    val grid = setup.grid!!
    val lines: List<List<MapAreaDefinition?>> = grid.rows() + grid.columns() + grid.diagonals()
    return lines.flatMap(::adjacentPairs)
        .map { it.map { it.asClassDeclaration.name } }
        .flatMap {
          val (one, two) = it
          listOf(
              border.specialize(one.type, two.type),
              border.specialize(two.type, one.type),
          )
        }
  }

  private fun adjacentPairs(it: List<MapAreaDefinition?>): List<List<MapAreaDefinition>> =
      it.windowed(2).filterNoNulls()

}
