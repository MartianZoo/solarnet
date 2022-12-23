package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.pets.PetsParser.parse
import org.junit.jupiter.api.Test

class DeprodifierTest {
  val resources = setOf(
      "StandardResource",
      "Megacredit",
      "Steel",
      "Titanium",
      "Plant",
      "Energy",
      "Heat")

  val prodType = "Production"

  @Test fun noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = parse(s)
    val ep: Effect = deprodify(e, resources, prodType)
    assertThat(ep.toString()).isEqualTo(s)
  }

  @Test fun simpleProd() {
    val prodden: Effect = parse("This: PROD[Plant / PlantTag]")
    val deprodded: Effect = deprodify(prodden, resources, prodType)
    assertThat(deprodded.toString()).isEqualTo("This: Production<Plant> / PlantTag")
  }
}
