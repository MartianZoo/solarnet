package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.petaform.api.PetaformNode
import org.junit.jupiter.api.Test

// Not testing much, just a bit of the canon data
class ComponentTypeTest {
  @Test fun foo() {
    val data = Canon.componentTypeData
    val tr = data["TerraformRating"]!!
    assertThat(tr.name).isEqualTo("TerraformRating")
    assertThat(tr.abstract).isFalse()
    assertThat(tr.supertypesPetaform).containsExactly("Owned<Player>", "Plural").inOrder()
    assertThat(tr.dependenciesPetaform).isEmpty()
    assertThat(tr.effectsPetaform).containsExactly("ProductionPhase: 1", "End: VictoryPoint")
  }

  @Test fun slurp() {
    val table = ComponentTable()
    table.addAll(Canon.componentTypeData.values)
    table.addAll(Canon.mapData.values.flatMap { it })
    table.addAll(Canon.cardData.values)
    table.all().forEach { rc ->
      val cc = rc.data
      checkRoundTrip(cc.supertypesPetaform, rc.supertypes)
      checkRoundTrip(cc.dependenciesPetaform, rc.dependencies)
      checkRoundTrip(listOfNotNull(cc.immediatePetaform), listOfNotNull(rc.immediate))
      checkRoundTrip(cc.actionsPetaform, rc.actions)
      checkRoundTrip(cc.effectsPetaform, rc.effects)
    }
    assertThat(table.all().size).isGreaterThan(580)
  }

  fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetaformNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }
}
