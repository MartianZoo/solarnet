package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CyberiaSystemsTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame()
    kim.exMachina("$Mine, $IndustrialMicrobes")
  }

  @Test
  internal fun `Copies production boxes from two different building cards`() {
    kim.playProject(CyberiaSystems, 16) {
          doTask("CopyProductionBox<$Mine>")
          doTask("CopyProductionBox<$IndustrialMicrobes>")
        }
        .expect("PROD[3 Steel, Energy]")
  }

  @Test
  internal fun `Cannot copy the same card twice`() {
    shouldThrow<NarrowingException> {
      kim.playProject(CyberiaSystems, 16) {
        doTask("CopyProductionBox<$Mine>")
        doTask("CopyProductionBox<$Mine>")
      }
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    shouldThrow<NarrowingException> {
      kim.playProject(CyberiaSystems, 16) { doTask("CopyProductionBox<$CyberiaSystems>") }
    }
  }

  @Test
  internal fun `Reducing then restoring energy production gives Manutech energy immediately`() {
    newTestGame(addOptions = "IndustrialComplex", kimCorporation = Manutech)
    kim.exMachina("$MagneticFieldGeneratorsPromo, $IndustrialComplex")
    kim.setToExMachina(4, "PROD[Energy]")
    kim.setToExMachina(0, "Energy")
    kim.setToExMachina(16, "MC")

    kim.playProject(CyberiaSystems, 16) {
          doTask("CopyProductionBox<$MagneticFieldGeneratorsPromo>")
          doTask("CopyProductionBox<$IndustrialComplex>")
        }
        .expect("2 Energy, 2 Plant, PROD[-2 Energy, 2 Plant]")
  }
}
