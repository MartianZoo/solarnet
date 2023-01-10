package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.SpecialComponent.Component
import java.util.TreeSet
import org.junit.jupiter.api.Test

private class PetClassCanonTest {

  @Test
  fun component() {
    val table = PetClassLoader(Canon)

    table.load(Component.name).apply {
      assertThat(name).isEqualTo("Component")
      assertThat(abstract).isTrue()
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).isEmpty()
      assertThat(directSuperclasses).isEmpty()
    }
    assertThat(table.loadedClasses().size).isEqualTo(1)

    table.load("OceanTile").apply {
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).containsExactly(Dependency.Key(table["Tile"], 0))
      assertThat(directSuperclasses.map { it.name }).containsExactly("GlobalParameter", "Tile")
          .inOrder()
      assertThat(allSuperclasses.map { it.name }).containsExactly("Component",
          "GlobalParameter",
          "Tile",
          "OceanTile").inOrder()
      assertThat(table.loadedClasses().size).isEqualTo(4)

      assertThat(baseType).isEqualTo(table.resolve("OceanTile<MarsArea>"))
    }
    assertThat(table.loadedClasses().size).isEqualTo(6)
  }

  @Test
  fun slurp() {
    val table = PetClassLoader(Canon).loadEverything()
    val all = table.loadedClassNames().map { table[it] }

    all.forEach { it.effectsRaw.forEach(::testRoundTrip) }
    all.forEach { it.baseType }
    all.forEach { it.effects.forEach(::testRoundTrip) }
  }

  @Test
  fun intersectionTypes() {
    val table = PetClassLoader(Canon).loadEverything()

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    assertThat(table["OwnedTile"].intersectionType).isTrue()

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    assertThat(table["ActionCard"].intersectionType).isTrue()
  }

  fun findValidTypes() {
    val table = PetClassLoader(Canon).loadEverything()
    val all = table.loadedClassNames().map { table[it] }

    val names: List<String> = all.map { it.name }.filterNot {
      it.matches(Regex("^Card.{3,4}$")) && it.hashCode() % 12 != 0
    }.filterNot {
      it.matches(Regex("^(Tharsis|Hellas|Elysium)")) && it.hashCode() % 8 != 0
    }.filterNot {
      it in setOf("Component", "Die")
    }

    val abstracts = TreeSet<String>()
    val concretes = TreeSet<String>()
    val invalids = TreeSet<String>()

    while (abstracts.size < 100 || concretes.size < 100 || invalids.size < 100) {
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
        try {
          val type = table.resolve(thing)
          if (type.abstract) {
            if (abstracts.size < 100) abstracts.add(thing)
          } else {
            if (concretes.size < 100) concretes.add(thing)
          }
        } catch (e: Exception) {
          if (invalids.size < 100) invalids.add(thing)
        }
      }
    }
    println("ABSTRACTS")
    abstracts.forEach(::println)
    println()
    println("CONCRETES")
    concretes.forEach(::println)
    println()
    println("INVALIDS")
    invalids.forEach(::println)
  }

  fun describeEverything() {
    val table = PetClassLoader(Canon).loadEverything()
    val all = table.loadedClassNames().map { table[it] }
    all.sortedBy { it.name }.forEach { c ->
      val interestingSupes = c.allSuperclasses.filter {
        it.name !in setOf(Component.name, c.name)
      }
      println("${c.baseType} : $interestingSupes")
    }
  }
}
