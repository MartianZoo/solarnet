package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.Deprodifier.Companion.deprodify
import dev.martianzoo.tfm.pets.Effect
import org.junit.jupiter.api.Test

class PetClassCanonTest {
  @Test
  fun spew() {
    val defs = Canon.allDefinitions
    val cl = PetClassLoader(defs)
    cl.loadAll()
    cl.all().sortedBy { it.name }.forEach {
      println("${it.name} : ${it.directSuperclasses} : ${it.directEffects}")
    }
  }

  fun deprodify(e: Effect): Effect {
    return deprodify(e, resources, prodType)
  }

  val resources = setOf(
      "StandardResource",
      "Megacredit",
      "Steel",
      "Titanium",
      "Plant",
      "Energy",
      "Heat")

  val prodType = "Production"
}
