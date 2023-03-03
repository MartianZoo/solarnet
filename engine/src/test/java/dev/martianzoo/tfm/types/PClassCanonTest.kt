package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.random
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/** Tests of [PClass] that use the [Canon] dataset because it's convenient. */
private class PClassCanonTest {

  @Test
  fun component() {
    val table = PClassLoader(Canon)

    table.componentClass.apply {
      assertThat(className).isEqualTo(COMPONENT)
      assertThat(abstract).isTrue()
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).isEmpty()
      assertThat(directSuperclasses).isEmpty()
    }

    table.load(cn("OceanTile")).apply {
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).containsExactly(Key(cn("Tile"), 0))
      assertThat(directSuperclasses.toStrings())
          .containsExactly("GlobalParameter", "Tile")
          .inOrder()
      assertThat(allSuperclasses.toStrings())
          .containsExactly("Component", "GlobalParameter", "Tile", "OceanTile")
          .inOrder()

      table.load(cn("MarsArea"))
      assertThat(baseType).isEqualTo(table.resolveType(typeExpr("OceanTile<MarsArea>")))
    }
  }

  @Disabled
  @Test
  fun twoDeps() {
    val table = PClassLoader(Canon).loadEverything()
    val map = mutableMapOf<List<Key>, MutableList<ClassName>>()
    table.allClasses.forEach {
      if (it.allDependencyKeys.size >= 2) {
        val key = it.allDependencyKeys.toList()
        map.putIfAbsent(key, mutableListOf())
        map[key]!! += it.className
      }
    }
    map.forEach { (k, v) -> println("$k : $v") }
  }

  @Test
  fun canGetBaseTypes() {
    val table = PClassLoader(Canon).loadEverything()
    table.allClasses.forEach { it.baseType }
  }

  @Test
  fun getClassEffects() {
    val table = PClassLoader(Canon).loadEverything()
    table.allClasses.forEach { it.classEffects }
  }

  @Test
  fun classInvariants() {
    val table = PClassLoader(Canon).loadEverything()
    val temp = table.getClass(cn("TemperatureStep"))
    assertThat(temp.invariants).containsExactly(requirement("MAX 19 This"))
    val ocean = table.getClass(cn("OceanTile"))
    assertThat(ocean.invariants).containsExactly(requirement("MAX 9 OceanTile"))
    val area = table.getClass(cn("Area"))
    assertThat(area.invariants)
        .containsExactly(
            requirement("MAX 1 This"),
            requirement("MAX 1 Tile<This>"),
        )
  }

  @Disabled
  @Test
  fun describeManyClasses() {
    val table = PClassLoader(Canon).loadEverything()
    val all: Set<PClass> = table.allClasses
    val random = random(all, 80)
    random.forEach {
      // println(it.describe())
      println()
    }
  }

  @Test
  fun checkTypes() {
    val table = PClassLoader(Canon).loadEverything()

    table.allClasses.forEach { pclass ->
      pclass.classEffects.forEach {
        table.checkAllTypes(it.replaceAll(THIS.type, pclass.className.type))
      }
    }
  }

  @Disabled
  @Test
  fun findValidTypes() {
    val table = PClassLoader(Canon).loadEverything()

    val names: List<ClassName> =
        table.allClasses
            .classNames()
            .filterNot { it.matches(Regex("^Card.{3,4}$")) && it.hashCode() % 12 != 0 }
            .filterNot { it.matches(Regex("^(Tharsis|Hellas|Elysium)")) && it.hashCode() % 8 != 0 }
            .filterNot { it in setOf(COMPONENT, DIE) }

    val abstracts = sortedSetOf<String>()
    val concretes = sortedSetOf<String>()
    val invalids = sortedSetOf<String>()

    while (abstracts.size < 100 || concretes.size < 100 || invalids.size < 100) {
      val name1 = names.random()
      val name2 = names.random()
      val name3 = names.random()
      val name4 = names.random()
      val tryThese =
          setOf(
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
          val ptype = table.resolveType(typeExpr(thing))
          if (ptype.abstract) {
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
}
