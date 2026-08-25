package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class EnergyTappingTest : CardTest() {
  // With no other energy-production target, the increase makes the decrease executable.
  @Test
  internal fun `Can be played when Manutech offsets its production loss`() {
    newGame(VenusNextExpansion)
    p1.manual("$Manutech")
    p1.manual("$EnergyTapping").expect("Energy")
    p1.assertProds(0 to "Energy")
  }
}
