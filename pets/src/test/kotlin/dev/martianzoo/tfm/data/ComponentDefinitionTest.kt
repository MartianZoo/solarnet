package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.PetsNode
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.types.PetClassLoader
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentDefinitionTest {
  @Test
  fun foo() {
    val tr = Canon.componentDefinitions["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypes.map(Any::toString)).containsExactly("Owned<Player>")
    assertThat(tr.dependencies).isEmpty()
    assertThat(tr.effects.map(Any::toString)).containsExactly("ProductionPhase: 1", "End: VictoryPoint")
  }

  @Test
  fun slurp() {
    val defns = Canon.allDefinitions
    assertThat(defns.size).isGreaterThan(550)

    val table = PetClassLoader(defns)
    for (defn in defns.values) {
      println(defn)
      val clazz = table.getOrDefine(defn.name)
      clazz.directEffects.forEach { testRoundTrip(it) }
    }
  }

  @Test fun extendsConcrete() {
    val table = PetClassLoader(Canon.allDefinitions)
    table.loadAll()
    val extendsNonabstract = table.all().flatMap {
      clazz -> clazz.directSuperclasses.filterNot { it.abstract }.map { it.name to clazz.name }
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
