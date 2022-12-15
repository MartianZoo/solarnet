package dev.martianzoo.tfm.data

import com.google.common.collect.MultimapBuilder
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.PetsNode
import dev.martianzoo.tfm.types.ComponentClassLoader
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentDefinitionTest {
  @Test
  fun foo() {
    val tr = Canon.componentDefinitions["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypesText).containsExactly("Owned<Player>")
    assertThat(tr.dependenciesText).isEmpty()
    assertThat(tr.effectsText).containsExactly("ProductionPhase: 1", "End: VictoryPoint")
  }

  @Test
  fun slurp() {
    val defns = Canon.allDefinitions
    assertThat(defns.size).isGreaterThan(550)

    val table = ComponentClassLoader()
    table.loadAll(defns.values)

    table.all().forEach { clazz ->
      val def = defns[clazz.name]!!
      if (def.supertypesText.isNotEmpty()) {
        // checkRoundTrip(cc.supertypesText, rc.superclasses)
      }
      checkRoundTrip(listOfNotNull(def.immediateText), listOfNotNull(clazz.immediate))
      checkRoundTrip(def.actionsText, clazz.actions)
      checkRoundTrip(def.effectsText, clazz.effects)
      // deps??
    }
  }

  @Test fun nuts() {
    val table = ComponentClassLoader()
    table.loadAll(Canon.allDefinitions.values)

    val mmap = MultimapBuilder.treeKeys().hashSetValues().build<String, PetsNode>()
    table.all().forEach { clazz ->
      (clazz.effects + clazz.actions + listOfNotNull(clazz.immediate))
          .map { Vanillafier.san(it) }
          .flatMap { listOf(it) + it.descendants() }
          .forEach { mmap.put(it::class.qualifiedName, it) }
    }

    mmap.keySet().forEach {
      println(it)
      println()
      mmap.get(it).map { it.toString() }.sorted().forEach(::println)
      println()
      println()
    }
  }

  @Test fun extendsNonabstract() {
    val table = ComponentClassLoader()
    table.loadAll(Canon.allDefinitions.values)
    val extendsNonabstract = table.all().flatMap {
      clazz -> clazz.superclasses.filterNot { it.abstract }.map { it.name to clazz.name }
    }
    assertThat(extendsNonabstract).containsExactly("CityTile" to "Tile008")
  }

  fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetsNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }
}
