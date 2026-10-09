package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class EnergyTappingTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(kimCorporation = Manutech)

  // With no other energy-production target, the increase makes the decrease executable.
  @Test
  internal fun `Can gain the energy production it must then lose and still pay Manutech`() {
    kim.setToExMachina(3, "MC")
    stan.setToExMachina(0, "PROD[Energy]")
    rob.setToExMachina(0, "PROD[Energy]")

    kim.playProject(EnergyTapping, 3).expect("Energy, PROD[0 Energy]")
  }
}
