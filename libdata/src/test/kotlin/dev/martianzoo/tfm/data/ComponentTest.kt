package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.petaform.api.PetaformNode
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
  }

  @Test fun slurp() {
    val table = ComponentTable()
    val list: List<TfmObject> =
        Canon.componentClassData.values +
        Canon.mapData.values.flatMap { it } +
        Canon.cardData.values
    list.forEach { obj ->
      table.add(obj)
      val cc = obj.asComponent
      val rc = table[cc.name]!!
      checkRoundTrip(cc.supertypesPetaform, rc.supertypes)
      checkRoundTrip(cc.dependenciesPetaform, rc.dependencies)
      checkRoundTrip(listOfNotNull(cc.immediatePetaform), listOfNotNull(rc.immediate))
      checkRoundTrip(cc.actionsPetaform, rc.actions)
      checkRoundTrip(cc.effectsPetaform, rc.effects)
    }
  }

  fun checkRoundTrip(source: Collection<String>, cooked: Collection<PetaformNode>) {
    assertThat(source.size).isEqualTo(cooked.size)
    source.zip(cooked).forEach {
      assertThat("${it.second}").isEqualTo(it.first)
    }
  }
}
