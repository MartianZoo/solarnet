package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
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
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "-16 MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
          "CopyProductionBox<Mine>",
          "PROD[Steel]",
          "CopyProductionBox<IndustrialMicrobes>",
          "PROD[Energy]",
          "PROD[Steel]",
      )
    }
  }

  @Test
  internal fun `Cannot copy the same card twice`() {
    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<CyberiaSystems>>")
        selectTask("-X? MC")
        doTasks("-16 MC", "CyberiaSystems FROM ProjectCard")
        selectTask("PROD[Steel]")
        doTask("CopyProductionBox<Mine>")
        selectTask("PROD[Steel]")
        selectOnlyTask()
        doTask("CopyProductionBox<Mine>")
      }
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<CyberiaSystems>>",
            "-16 MC",
            "CyberiaSystems FROM ProjectCard",
            "PROD[Steel]",
        )
        doTask("CopyProductionBox<CyberiaSystems>")
      }
    }
  }

  @Test
  internal fun `Reducing then restoring energy production gives Manutech energy immediately`() {
    newTestGame(addOptions = "IndustrialComplex", kimCorporation = Manutech)
    kim.exMachina("$MagneticFieldGeneratorsPromo, $IndustrialComplex")
    kim.setToExMachina(4, "PROD[Energy]")
    kim.setToExMachina(0, "Energy")
    kim.setToExMachina(16, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "-16 MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
          "Steel",
          "CopyProductionBox<MagneticFieldGeneratorsPromo>",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "2 Plant",
          "CopyProductionBox<IndustrialComplex>",
      )
      selectOnlyTask()
      doTasks(
          "PROD[MC]",
          "PROD[Titanium]",
          "PROD[2 Energy]",
          "PROD[Heat]",
          "MC",
          "Titanium",
          "2 Energy",
          "Heat",
      )
    }
  }
}
