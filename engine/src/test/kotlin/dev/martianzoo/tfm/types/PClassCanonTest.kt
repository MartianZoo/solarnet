package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.DIE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.util.toStrings
import java.util.TreeSet
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/** Tests of [PClass] that use the [Canon] dataset because it's convenient. */
private class PClassCanonTest {

  @Test
  fun component() {
    val table = PClassLoader(Canon)

    table.componentClass.apply {
      assertThat(name).isEqualTo(COMPONENT)
      assertThat(abstract).isTrue()
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).isEmpty()
      assertThat(directSuperclasses).isEmpty()
    }

    table.load(cn("OceanTile")).apply {
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).containsExactly(Dependency.Key(cn("Tile"), 0))
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

  @Test
  fun canGetBaseTypes() {
    val table = PClassLoader(Canon).loadEverything()
    table.allClasses.forEach { it.baseType }
  }

  @Test
  fun getGetClassEffects() {
    val table = PClassLoader(Canon).loadEverything()
    table.allClasses.forEach { it.classEffects }
  }

  @Disabled
  @Test
  fun findValidTypes() {
    val table = PClassLoader(Canon).loadEverything()

    val names: List<ClassName> =
        table.allClasses
            .map { it.name }
            .filterNot { it.matches(Regex("^Card.{3,4}$")) && it.hashCode() % 12 != 0 }
            .filterNot { it.matches(Regex("^(Tharsis|Hellas|Elysium)")) && it.hashCode() % 8 != 0 }
            .filterNot { it in setOf(COMPONENT, DIE) }

    val abstracts = TreeSet<String>()
    val concretes = TreeSet<String>()
    val invalids = TreeSet<String>()

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
