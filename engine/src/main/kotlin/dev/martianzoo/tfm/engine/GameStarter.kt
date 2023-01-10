package dev.martianzoo.tfm.engine

import com.google.common.collect.HashMultiset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Bundle
import dev.martianzoo.tfm.canon.Canon.Bundle.Base
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.util.Debug.d
import dev.martianzoo.util.onlyElement

object GameStarter {
  internal fun newGame(players: Int, bundles: Set<Bundle>): Game {
    require(Base in bundles)
    val bundleCodes = bundles.map { "${it.id}" }.toSet()
    val marsMap = bundles.filter { it.isMap }.onlyElement()

    val loader = PetClassLoader(Canon)
    loader.autoLoadDependencies = true

    d("\nLoading players\n")
    for (i in 1..players) {
      loader.load("Player$i")
    }

    d("\nLoading map\n")
    loader.load(marsMap.name)
    loader.loadAll(Canon.getMap(marsMap).map { it.className }.sorted())

    println("\nLoading cards\n")
    val cards = Canon.cardDefinitions.filter { it.bundle in bundleCodes }
    loader.loadAll(cards.map { it.className }.sorted())

    println("\nLoading milestones\n")
    val allMiles = Canon.milestoneDefinitions.filter { it.bundle in bundleCodes }
    loader.loadAll(allMiles.map { it.className }.sorted())

    println("\nLoading standard actions\n")
    val acts = Canon.actionDefinitions.filter { it.bundle in bundleCodes }
    loader.loadAll(acts.map { it.className }.sorted())

    // Hack because we haven't deprodified yet
    loader.loadAll("Production")

    loader.freeze()

    val multiset = HashMultiset.create<Component>()
    loader.loadedClasses().forEach {
      if (it.isSingleton() && !it.baseType.abstract) {
        Component(loader.resolve(gte(it.name)))
      }
    }
    return Game(Canon, ComponentGraph(multiset), loader)
  }
}
