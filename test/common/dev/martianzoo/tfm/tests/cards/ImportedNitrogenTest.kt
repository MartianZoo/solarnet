package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ImportedNitrogenTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Unstorable microbes and animals give no Topsoil Contract or Meat Industry payout`() {
    kim.exMachina("$TopsoilContract, $MeatIndustry")
    kim.setToExMachina(23, "MC")
    // Opponent-owned holders cannot receive the imported resources.
    stan.exMachina("$Tardigrades, $Pets")

    kim.inTurn {
          doTasks("PlayProject<Class<ImportedNitrogen>>", "-23 MC")
          selectTask("-X Titanium?")
          doTasks("Ok", "ImportedNitrogen FROM ProjectCard", "TerraformRating", "4 Plant")
          selectTask("3 Microbe.")
          selectTask("2 Animal.")
        }
        .expect(
            "TerraformRating, 4 Plant, 0 Microbe, 0 Animal, -23 MC, " +
                "0 Microbe<Stan>, 0 Animal<Stan>"
        )
  }

  @Test
  internal fun `Microbes and animals choose separate own holders and pay their gain effects`() {
    kim.exMachina(
        "$TopsoilContract, $MeatIndustry, $Tardigrades, $NitriteReducingBacteria, $Pets, $Vermin"
    )
    kim.setToExMachina(23, "MC")

    kim.inTurn {
          doTasks("PlayProject<Class<ImportedNitrogen>>", "-23 MC")
          selectTask("-X Titanium?")
          doTasks(
              "Ok",
              "ImportedNitrogen FROM ProjectCard",
              "TerraformRating",
              "4 Plant",
              "3 Microbe<Tardigrades>.",
              "3 MC",
          )
          selectTask("2 Animal.")
          doTasks("2 Animal<Pets>.", "4 MC")
        }
        .expect(
            "TerraformRating, 4 Plant, 3 Microbe<$Tardigrades>, 2 Animal<$Pets>, " +
                "0 Microbe<$NitriteReducingBacteria>, 0 Animal<$Vermin>, -16 MC"
        )
  }

  @Test
  internal fun `Cannot split imported microbes between holders`() {
    kim.exMachina("$Tardigrades, $NitriteReducingBacteria")
    kim.setToExMachina(23, "MC")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<ImportedNitrogen>>", "-23 MC")
        selectTask("-X Titanium?")
        selectTask("Ok")
        doTask("ImportedNitrogen FROM ProjectCard")
        selectTask("TerraformRating!")
        selectTask("4 Plant!")
        selectTask("2 Animal.")
        selectTask("3 Microbe.")
        doTask("Microbe<Tardigrades>")
      }
    }
  }
}
