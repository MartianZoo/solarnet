package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.PetaformObject
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentTest {
  @Test fun foo() {
    val data = Canon.componentClassData
    val tr = data["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.system).isFalse()
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypesPetaform).containsExactly("Owned<Player>", "Plural").inOrder()
    assertThat(tr.dependenciesPetaform).isEmpty()
    assertThat(tr.effectsPetaform).containsExactly("ProductionPhase: 1", "End: VictoryPoint")

    assertThat(tr.supertypes).containsExactly(
        Expression("Owned", Expression("Player")), Expression("Plural")).inOrder()
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
      (cpt.supertypesPetaform + cpt.dependenciesPetaform + cpt.effectsPetaform).flatMap {
        it.split(Regex("[\\W\\d]+"))
      }.filterNot { it == "" }.toHashSet()

  @Test fun slurp() {
    Canon.componentClassData.values.forEach { cc ->
      checkRoundTrip(cc.supertypesPetaform, cc.supertypes)
      checkRoundTrip(cc.dependenciesPetaform, cc.dependencies)
      checkRoundTrip(cc.effectsPetaform, cc.effects)
    }
  }

  fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetaformObject>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat(it.second.petaform).isEqualTo(it.first)
    }
  }
}
