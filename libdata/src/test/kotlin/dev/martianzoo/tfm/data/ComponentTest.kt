package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentTest {
  @Test fun foo() {
    val data = Canon.componentClassData
    val tr = data["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.system).isFalse()
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypes).containsExactly("Owned<Player>", "Plural").inOrder()
    assertThat(tr.dependencies).isEmpty()
    assertThat(tr.effects).containsExactly("ProductionPhase: 1", "End: VictoryPoint")
  }

  // We might have to go circular some day, but not yet
  @Test fun noForwardRefs() {
    val sofar = mutableSetOf("Ctype", "This")
    Canon.componentClassData.forEach { (name, cpt) ->
      sofar += name
      assertThat(sofar).containsAtLeastElementsIn(pullNamesOutOf(cpt))
    }
  }

  private fun pullNamesOutOf(cpt: Component) =
      (cpt.supertypes + cpt.dependencies + cpt.effects).flatMap {
        it.split(Regex("[\\W\\d]+"))
      }.filterNot { it == "" }.toHashSet()
}
