package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import dev.martianzoo.tfm.data.ComponentClassDefinition as CCD

class ComponentTypesTest {
  @Test
  fun what() {
    val loader = ComponentClassLoader()

    loader.load(CCD("Component"))
    loader.load(CCD("Anyone"))
    loader.load(CCD("Player", supertypesPetaform=setOf("Anyone")))
    loader.load(CCD("Owned", dependenciesPetaform=listOf("Anyone")))
    loader.load(CCD("VictoryPoint", supertypesPetaform=setOf("Owned<Player>")))

    val vp: ComponentType = loader.resolve("VictoryPoint")
    assertThat(vp.isSubtypeOf(loader.resolve("Component")))
    assertThat(vp.componentClass.name).isEqualTo("VictoryPoint")
    assertThat(vp.isSubtypeOf(loader.resolve("Owned<Anyone>")))
    assertThat(vp.isSubtypeOf(loader.resolve("Owned<Player>")))
    assertThat(vp.dependencies.map).hasSize(1)
    assertThat(vp.dependencies.map.values).containsExactly(loader.resolve("Player"))

  }
}
