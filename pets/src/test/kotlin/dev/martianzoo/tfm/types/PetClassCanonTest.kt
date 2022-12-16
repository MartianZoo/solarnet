package dev.martianzoo.tfm.types

import com.google.common.truth.Truth
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.testRoundTrip
import org.junit.jupiter.api.Test

class PetClassCanonTest {
  @Test
  fun spew() {
    val defs = Canon.allDefinitions
    val cl = PetClassLoader(defs)
    cl.loadAll()
    cl.all().sortedBy { it.name }.forEach {
      println("${it.name} : ${it.directSuperclasses} : ${it.directEffects}")
    }
  }

  @Test
  fun slurp() {
    val defns = Canon.allDefinitions
    Truth.assertThat(defns.size).isGreaterThan(550)

    val table = PetClassLoader(defns)
    table.loadAll()

    table.all().forEach {
      clazz -> clazz.directEffects.forEach { testRoundTrip(it) }
    }
  }

  @Test fun subConcrete() {
    val table = PetClassLoader(Canon.allDefinitions)
    table.loadAll()
    val subConcrete = table.all().flatMap { clazz ->
      clazz.directSuperclasses.filterNot { it.abstract }.map { clazz.name to it.name }
    }

    // only one case of subclassing a concrete class in the whole canon
    Truth.assertThat(subConcrete).containsExactly("Tile008" to "CityTile")
  }
}
