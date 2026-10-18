package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CyberiaSystemsTest : TfmSandboxTest() {

  @Test
  internal fun `Copies Research Network using its wild Building tag`() {
    newTestGame("ResearchNetwork")
    kim.exMachina("ResearchNetwork, Mine")
    copyResearchNetwork().expect("PROD[MC]")
  }

  private fun copyResearchNetwork() = kim.inTurn {
    doTasks(
        "PlayProject<Class<CyberiaSystems>>",
        "16 Pay<> FROM MC",
        "CyberiaSystems FROM ProjectCard",
        "PROD[Steel]",
        "CopyProductionBox<Mine>",
        "PROD[Steel]",
    )
    selectTask("EACH @WildTagElection(HAS MAX 0 Tag) { Tag<@WildTagElection>? }")
    doTask("BuildingTag<WildTagElection<WildTag<ResearchNetwork>>>")
    doTasks("CopyProductionBox<ResearchNetwork>", "PROD[MC]")
  }

  @Test
  internal fun `Research Network stays Building while the second copied box counts tags`() {
    newTestGame("ResearchNetwork")
    kim.exMachina("ResearchNetwork, MedicalLab")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<CyberiaSystems>>",
              "16 Pay<> FROM MC",
              "CyberiaSystems FROM ProjectCard",
              "PROD[Steel]",
          )
          selectTask("EACH @WildTagElection(HAS MAX 0 Tag) { Tag<@WildTagElection>? }")
          doTask("BuildingTag<WildTagElection<WildTag<ResearchNetwork>>>")
          doTasks(
              "CopyProductionBox<ResearchNetwork>",
              "PROD[MC]",
              "CopyProductionBox<MedicalLab>",
              "PROD[1 MC]",
          )
        }
        .expect("PROD[Steel, 2 MC]")
  }

  @Test
  internal fun `Copied Gyropolis counts an Earth assignment`() {
    newTestGame("ResearchCoordination, Gyropolis")
    kim.exMachina("ResearchCoordination, Gyropolis, Mine")
    kim.setToExMachina(2, "PROD[Energy]")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<CyberiaSystems>>",
              "16 Pay<> FROM MC",
              "CyberiaSystems FROM ProjectCard",
              "PROD[Steel]",
          )
          selectTask("EACH @WildTagElection(HAS MAX 0 Tag) { Tag<@WildTagElection>? }")
          doTask("EarthTag<WildTagElection<WildTag<ResearchCoordination>>>")
          doTasks(
              "CopyProductionBox<Mine>",
              "PROD[Steel]",
              "CopyProductionBox<Gyropolis>",
              "PROD[-2 Energy]",
              "PROD[0 MC]",
              "PROD[1 MC]",
          )
        }
        .expect("PROD[2 Steel, -2 Energy, 1 MC]")
  }

  private fun initializeGame() {
    newTestGame()
    kim.exMachina("$Mine, $IndustrialMicrobes")
  }

  @Test
  internal fun `Copies production boxes from two different building cards`() {
    initializeGame()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CyberiaSystems>>",
          "16 Pay<> FROM MC",
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
    initializeGame()
    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<CyberiaSystems>>")
        doTasks("16 Pay<> FROM MC", "CyberiaSystems FROM ProjectCard")
        doTask("PROD[Steel]")
        doTask("CopyProductionBox<Mine>")
        doTask("PROD[Steel]")
        doTask("CopyProductionBox<Mine>")
      }
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    initializeGame()
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<CyberiaSystems>>",
            "16 Pay<> FROM MC",
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
          "16 Pay<> FROM MC",
          "CyberiaSystems FROM ProjectCard",
          "PROD[Steel]",
          "Steel",
          "CopyProductionBox<MagneticFieldGeneratorsPromo>",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "2 Plant",
          "CopyProductionBox<IndustrialComplex>",
      )
      // Expand the copied production box into its separate resource increases.
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
