package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class RoboticWorkforceTest : TfmSandboxTest() {
  @Test
  internal fun `Copies every resource in Strip Mine's production box`() {
    newTestGame()
    kim.exMachina("$StripMine")
    kim.setToExMachina(2, "PROD[Energy]")

    kim.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$StripMine>") }
        .expect("PROD[2 Steel, Titanium, -2 Energy]")
  }

  @Test
  internal fun `Cannot copy a non-building card`() {
    newTestGame()
    kim.exMachina("$Mine, $MassConverter")

    shouldThrow<NarrowingException> {
      kim.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$MassConverter>") }
    }
  }

  @Test
  internal fun `Cannot copy another player's building card`() {
    newTestGame()
    kim.exMachina("$IndustrialMicrobes")
    stan.exMachina("$Mine")

    shouldThrow<NarrowingException> {
      kim.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$Mine<Stan>>") }
    }
  }

  @Test
  internal fun `Cannot copy an absent building card`() {
    newTestGame()
    kim.exMachina("$IndustrialMicrobes")

    shouldThrow<NarrowingException> {
      kim.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$Mine>") }
    }
  }

  // https://boardgamegeek.com/thread/3430226/article/45396575#45396575
  @Test
  internal fun `Copies Industrial Complex's production adjustment without repeating its cost`() {
    newTestGame(addOptions = "IndustrialComplex")
    kim.exMachina("$IndustrialComplex")
    kim.setToExMachina(2, "PROD[Steel]")
    kim.setToExMachina(2, "PROD[Titanium]")
    kim.setToExMachina(2, "PROD[Plant]")
    kim.setToExMachina(2, "PROD[Energy]")
    kim.setToExMachina(2, "PROD[Heat]")

    kim.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$IndustrialComplex>") }
        .expect("-9 MC, PROD[MC, 0 Steel, 0 Titanium, 0 Plant, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Counts current building tags when copying Medical Lab`() {
    newTestGame()
    kim.playProject(MedicalLab, 13).expect("PROD[0 MC]")
    kim.exMachina("$Mine, $IndustrialMicrobes, $TitaniumMine")

    kim.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$MedicalLab>") }
        .expect("PROD[2 MC]")
  }

  @Test
  internal fun `Can choose a different victim when copying Heat Trappers`() {
    newTestGame()
    kim.setToExMachina(2, "PROD[Heat]")
    stan.setToExMachina(2, "PROD[Heat]")
    kim.playProject(HeatTrappers, 6) { doTask("PROD[-2 Heat<Stan>]") }
    stan.setToExMachina(2, "PROD[Heat]")

    kim.playProject(RoboticWorkforce, 9) {
          doTask("CopyProductionBox<$HeatTrappers>")
          doTask("PROD[-2 Heat<Kim>]")
        }
        .expect("PROD[-2 Heat<Kim>, Energy<Kim>, 0 Heat<Stan>]")
  }
}
