package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ImportedNitrogenTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Unstorable microbes and animals give no Topsoil Contract or Meat Industry payout`() {
    kim.setToExMachina(50, "MC")
    kim.playProject(TopsoilContract, 8)
    kim.playProject(MeatIndustry, 5)
    // Opponent-owned holders cannot receive the imported resources.
    stan.setToExMachina(14, "MC")
    stan.playProject(Tardigrades, 4)
    stan.playProject(Pets, 10)

    kim.playProject(ImportedNitrogen, 23)
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

    kim.playProject(ImportedNitrogen, 23) {
          addCardResources(Tardigrades)
          addCardResources(Pets)
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
      kim.playProject(ImportedNitrogen, 23) { doTask("Microbe<$Tardigrades>") }
    }
  }
}
