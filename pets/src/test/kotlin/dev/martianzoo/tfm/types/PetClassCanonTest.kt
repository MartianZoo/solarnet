package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test

class PetClassCanonTest {
  @Test
  fun spew() {
    val defs = Canon.allDefinitions
    val cl = PetClassLoader(defs)
    cl.loadAll()
    cl.all().sortedBy { it.name }.forEach {
      println("${it.name} : ${it.directSupertypes}")
    }
  }
}
