package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import kotlin.test.Test

class EnergyTappingTest : CardTest() {
  // FAQ: "raise your own energy production and then reduce it again"
  @Test
  fun `with Manutech, adds Energy Tapping`() {
    newGame("BMRV")
    p1.manual("Manutech")
    p1.manual("EnergyTapping").expect("Energy")
    p1.assertProds(0 to "Energy")
  }
}
