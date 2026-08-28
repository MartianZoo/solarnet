package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
