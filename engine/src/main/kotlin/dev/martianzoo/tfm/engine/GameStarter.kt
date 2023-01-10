package dev.martianzoo.tfm.engine

import com.google.common.collect.HashMultiset
import dev.martianzoo.tfm.canon.Canon.Bundle
import dev.martianzoo.tfm.canon.Canon.Bundle.Base
import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.SpecialComponent.Production
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.types.PetClassLoader

object GameStarter {
  internal fun newGame(
      authority: Authority,
      playerCount: Int,
      bundles: Set<Bundle>,
      // TODO extraCards: Set<CardDefinition>,
      // TODO replaceMilestones: Set<MilestoneDefinition>,
  ): Game {
    require(Base in bundles)
    val bundleCodes = bundles.map { it.id }.toSet()

    val loader = PetClassLoader(authority)
    loader.autoLoadDependencies = true

    for (seat in 1..playerCount) {
      loader.load("Player$seat")
    }

    val cards = authority.allDefinitions.filter { it.bundle in bundleCodes }
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
