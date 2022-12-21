package dev.martianzoo.tfm.types

import com.google.common.truth.Truth
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.testRoundTrip
import org.junit.jupiter.api.Test
import java.util.*

class PetClassCanonTest {
  @Test
  fun spew() {
    val table = PetClassLoader(Canon.allDefinitions).loadAll()
    table.all().sortedBy { it.name }.forEach {
      println("${it.name} : ${it.directSuperclasses} : ${it.directEffects}")
    }
  }

  @Test
  fun slurp() {
    val defns = Canon.allDefinitions
    Truth.assertThat(defns.size).isGreaterThan(650)

    val table = PetClassLoader(defns).loadAll()

    table.all().forEach {
      clazz -> clazz.directEffects.forEach { testRoundTrip(it) }
    }
  }

  @Test fun subConcrete() {
    val table = PetClassLoader(Canon.allDefinitions).loadAll()
    val subConcrete = table.all().flatMap { clazz ->
      clazz.directSuperclasses.filterNot { it.abstract }.map { clazz.name to it.name }
    }

    // only one case of subclassing a concrete class in the whole canon
    Truth.assertThat(subConcrete).containsExactly("Tile008" to "CityTile")
  }

  @Test fun findValidTypes() {
    val table = PetClassLoader(Canon.allDefinitions).loadAll()
    val names: List<String> = table.all().map { it.name }.filterNot {
      it.matches(Regex("^Card.{3,4}$")) && it.hashCode() % 6 != 0
    }.filterNot {
      it.matches(Regex("^(Tharsis|Hellas|Elysium)")) && it.hashCode() % 4 != 0
    }

    val abstracts = TreeSet<String>()
    val concretes = TreeSet<String>()

    while (abstracts.size < 200 || concretes.size < 200) {
      val name1 = names.random()
      val name2 = names.random()
      val name3 = names.random()
      val name4 = names.random()
      val tryThese = setOf(
          "$name1<$name2>",
          "$name1<$name2, $name3>",
          "$name1<$name2<$name3>>",
          "$name1<$name2, $name3, $name4>",
          "$name1<$name2<$name3>, $name4>",
          "$name1<$name2, $name3<$name4>>",
          "$name1<$name2<$name3<$name4>>>",
          "$name1<Player1>",
          "$name1<Player1, $name3>",
          "$name1<Player1, $name3, $name4>",
          "$name1<Player1, $name3<$name4>>",
          "$name1<$name2, Player1>",
          "$name1<$name2<Player1>>",
          "$name1<$name2, Player1, $name4>",
          "$name1<$name2<Player1>, $name4>",
          "$name1<$name2, $name3, Player1>",
          "$name1<$name2<$name3>, Player1>",
          "$name1<$name2, $name3<Player1>>",
          "$name1<$name2<$name3<Player1>>>",
      )
      for (thing in tryThese) {
        if (table.isValid(thing)) {
          val type = table.resolve(thing)
          if (type.abstract) {
            if (abstracts.size < 200) abstracts.add(thing)
          } else {
            if (concretes.size < 200) concretes.add(thing)
          }
        }
      }
    }
    println("ABSTRACTS")
    abstracts.forEach(::println)
    println()
    println("CONCRETES")
    concretes.forEach(::println)
  }
}
