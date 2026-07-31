package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class AphroditeTest : CardTest() {
  @Test
  fun `with Aphrodite owned by p2, p1 raises Venus`() {
    newGame("TerraformingMars,TharsisMapOption,VenusNextExpansion")
    val p2 = requireP2()
    p2.manual("Aphrodite")
    p1.manual("VenusStep").expect("2")
  }
}
