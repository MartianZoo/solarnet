package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class EnergyTappingTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(kimCorporation = Manutech)

  // With no other energy-production target, the increase makes the decrease executable.
  @Test
  internal fun `Can gain the energy production it must then lose and still pay Manutech`() {
    kim.setToExMachina(3, "MC")
    stan.setToExMachina(0, "PROD[Energy]")
    rob.setToExMachina(0, "PROD[Energy]")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<$EnergyTapping>>",
          "-3 MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "Energy", // Manutech gains the resource even though production is then lost.
          "PROD[-Energy]",
      )
    }
  }
}
