package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
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

    kim.inTurn {
      doTasks(
          "PlayProject<Class<RoboticWorkforce>>",
          "9 Pay<> FROM MC",
          "RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<StripMine>",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cannot copy a non-building card`() {
    newTestGame()
    kim.exMachina("$Mine, $MassConverter")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("PlayProject<Class<RoboticWorkforce>>")
        doTasks("9 Pay<> FROM MC", "RoboticWorkforce FROM ProjectCard")
        doTask("CopyProductionBox<MassConverter>")
      }
    }
  }

  @Test
  internal fun `Cannot copy another player's building card`() {
    newTestGame()
    kim.exMachina("$IndustrialMicrobes")
    stan.exMachina("$Mine")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<RoboticWorkforce>>")
        doTasks("9 Pay<> FROM MC", "RoboticWorkforce FROM ProjectCard")
        doTask("CopyProductionBox<Mine<Stan>>")
      }
    }
  }

  @Test
  internal fun `Cannot copy an absent building card`() {
    newTestGame()
    kim.exMachina("$IndustrialMicrobes")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("PlayProject<Class<RoboticWorkforce>>")
        doTasks("9 Pay<> FROM MC", "RoboticWorkforce FROM ProjectCard")
        doTask("CopyProductionBox<Mine>")
      }
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

    kim.inTurn {
          doTasks(
              "PlayProject<Class<RoboticWorkforce>>",
              "9 Pay<> FROM MC",
              "RoboticWorkforce FROM ProjectCard",
              "CopyProductionBox<IndustrialComplex>",
          )
          // Expand the copied production box into its separate resource increases.
          selectOnlyTask()
          doTask("PROD[MC]")
        }
        .expect("PROD[MC, 0 Steel, 0 Titanium, 0 Plant, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Counts current building tags when copying Medical Lab`() {
    newTestGame()
    kim.inTurn {
          doTasks("PlayProject<Class<MedicalLab>>", "13 Pay<> FROM MC")
          doTasks("MedicalLab FROM ProjectCard")
          doTask("PROD[0 MC]")
        }
        .expect("PROD[0 MC]")
    kim.exMachina("$Mine, $IndustrialMicrobes, $TitaniumMine")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<RoboticWorkforce>>",
          "9 Pay<> FROM MC",
          "RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<MedicalLab>",
      )
      doTask("PROD[2 MC]")
    }
  }

  @Test
  internal fun `Can choose a different victim when copying Heat Trappers`() {
    newTestGame()
    kim.setToExMachina(2, "PROD[Heat]")
    stan.setToExMachina(2, "PROD[Heat]")
    kim.inTurn {
      doTasks("PlayProject<Class<HeatTrappers>>", "6 Pay<> FROM MC")
      doTasks(
          "HeatTrappers FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-2 Heat<Stan>]",
      )
    }
    stan.setToExMachina(2, "PROD[Heat]")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<RoboticWorkforce>>",
              "9 Pay<> FROM MC",
              "RoboticWorkforce FROM ProjectCard",
              "CopyProductionBox<HeatTrappers>",
              "PROD[Energy]",
              "PROD[-2 Heat]",
          )
        }
        .expect("PROD[0 Heat<Stan>]")
  }
}
