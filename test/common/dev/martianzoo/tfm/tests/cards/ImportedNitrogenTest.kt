package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ImportedNitrogenTest : CardTest() {
  @Test
  internal fun `Unstorable microbes and animals give no Topsoil Contract or Meat Industry payout`() {
    newGame(PromoCardPack, CorporateEraExpansion)
    p1.playCorp(UnitedNationsMarsInitiative, 5)
    admin.phase("Action")
    val p2 = requireP2()
    p1.runOperation("50 MC")
    p1.playProject(TopsoilContract, 8)
    p1.playProject(MeatIndustry, 5)
    // Opponent-owned holders cannot receive the imported resources.
    p2.runOperation("14 MC, 2 ProjectCard")
    p2.playProject(Tardigrades, 4)
    p2.playProject(Pets, 10)

    p1.playProject(ImportedNitrogen, 23)
        .expect(
            "TerraformRating, 4 Plant, 0 Microbe, 0 Animal, -23 MC, " +
                "0 Microbe<Player2>, 0 Animal<Player2>"
        )
  }

  @Test
  internal fun `Microbes and animals choose separate own holders and pay their gain effects`() {
    newGame(PromoCardPack, CorporateEraExpansion)
    p1.playCorp(UnitedNationsMarsInitiative, 5)
    admin.phase("Action")
    p1.runOperation("100 MC, 2 ProjectCard")
    p1.playProject(TopsoilContract, 8)
    p1.playProject(MeatIndustry, 5)
    p1.playProject(Tardigrades, 4)
    p1.playProject(NitriteReducingBacteria, 11)
    p1.playProject(Pets, 10)
    p1.playProject(Vermin, 8)

    p1.playProject(ImportedNitrogen, 23) {
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
    newGame(CorporateEraExpansion)
    p1.runOperation("$Tardigrades, $NitriteReducingBacteria")

    shouldThrow<NarrowingException> {
      p1.runOperation("$ImportedNitrogen") { doTask("Microbe<$Tardigrades>") }
    }
  }
}
