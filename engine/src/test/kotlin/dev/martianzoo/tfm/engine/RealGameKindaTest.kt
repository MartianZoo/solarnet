package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Bundle
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.util.random
import org.junit.jupiter.api.Test

class RealGameKindaTest {
  @Test
  fun hereWeGo() {
    val all = Canon.allClassDeclarations.keys
    println()

    val loader = PetClassLoader(Canon)
    loader.autoLoadDependencies = true

    println("\nLoading players\n")
    loader.loadAll("Player1", "Player2")

    println("\nLoading map\n")
    loader.load("Tharsis")
    loader.loadAll(Canon.getMap(Bundle.THARSIS).map { it.className }.sorted())

    println("\nLoading cards\n")
    val nonVenusCards = Canon.cardDefinitions.filterNot { it.bundle == "V" }
    loader.loadAll(nonVenusCards.map { it.className }.sorted())

    println("\nLoading milestones\n")
    val allMiles = Canon.milestoneDefinitions.filterNot { it.bundle == "V" }
    loader.loadAll(random(allMiles, 5).map { it.className }.sorted())

    loader.freeze()

    val game = Game(Canon, ComponentGraph(), loader)
    loader.loadedClasses().forEach {
      if (it.isSingleton() && !it.baseType.abstract)
          game.applyChange(1, gaining = gte(it.name))
    }

    val venus = Canon.cardDefinitions.filter { it.bundle == "V" }.map { it.className }
    (all - venus - loader.loadedClassNames()).forEach(::println)
    // Border CityTag CorpCard Cost MarsMap Production! Row

    game.components.changeLog.forEach(::println)
  }
}
