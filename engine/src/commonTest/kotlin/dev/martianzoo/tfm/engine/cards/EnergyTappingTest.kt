package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import kotlin.test.Test

class EnergyTappingTest : CardTest() {
  // With no other energy-production target, the increase makes the decrease executable.
  @Test
  fun `with Manutech, adds Energy Tapping`() {
    newGame(VenusNextExpansion)
    p1.manual("Manutech")
    p1.manual("EnergyTapping").expect("Energy")
    p1.assertProds(0 to "Energy")
  }
}
