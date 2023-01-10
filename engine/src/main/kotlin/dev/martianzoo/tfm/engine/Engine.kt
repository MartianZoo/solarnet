package dev.martianzoo.tfm.engine

import com.google.common.collect.HashMultiset
import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.SpecialComponent.Production
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.types.PetClassLoader

object Engine {
  internal fun newGame(
      authority: Authority,
      playerCount: Int,
      bundles: Collection<String>,
      // TODO extraCards: Set<CardDefinition>,
      // TODO removeCards: Set<CardDefinition>,
      // TODO customMilestones: Set<MilestoneDefinition>,
  ): Game {
    val loader = PetClassLoader(authority)
    loader.autoLoadDependencies = true

    for (seat in 1..playerCount) {
      loader.load("Player$seat")
    }

    val cards = authority.allDefinitions.filter { it.bundle in bundles }
    loader.loadAll(cards.map { it.className }.sorted())

    // Hacks
    loader.loadAll(Production.name, "Border")

    loader.freeze()

    val multiset = HashMultiset.create<Component>()
    loader.loadedClasses().forEach {
      if (it.isSingleton() && !it.baseType.abstract) {
        Component(loader.resolve(gte(it.name)))
      }
    }
    return Game(authority, ComponentGraph(multiset), loader)
  }
}
