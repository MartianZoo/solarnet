package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class EnergyTappingTest : CardTest() {
  // With no other energy-production target, the increase makes the decrease executable.
  @Test
  internal fun `Can gain the energy production it must then lose and still pay Manutech`() {
    newGame(VenusNextExpansion, startingProjects = listOf(1))
    p1.playCorp(Manutech)
    admin.phase("Action")

    p1.playProject(EnergyTapping, 3).expect("Energy, PROD[0 Energy]")
    requireP2().assertProds(0 to "Energy")
  }
}
