package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.BusinessContacts
import dev.martianzoo.tfm.tests.cards.cardnames.DomeFarming
import dev.martianzoo.tfm.tests.cards.cardnames.IoResearchOutpost
import dev.martianzoo.tfm.tests.cards.cardnames.Pets
import dev.martianzoo.tfm.tests.cards.cardnames.PowerPlant
import dev.martianzoo.tfm.tests.cards.cardnames.Research
import dev.martianzoo.tfm.tests.cards.cardnames.Spire
import dev.martianzoo.tfm.tests.cards.cardnames.StandardTechnology
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class StandardTechnologyTest : CardTest() {
  @BeforeTest
  private fun initializeGame() {
    newGame()
    admin.phase("Action")
    p1.runOperation("$StandardTechnology")
  }

  @Test
  internal fun `Rebate cannot fund the triggering standard project`() {
    p1.runOperation("8 MC")

    shouldThrow<LimitsException> { p1.stdProject("PowerPlantProject") }

    p1.assertCounts(8 to "MC", 0 to "Owed", 0 to "ActionBilling")
    p1.assertProds(0 to "Energy")
  }

  @Test
  internal fun `Awards the rebate after paying for a standard project`() {
    p1.runOperation("11 MC")

    p1.stdProject("PowerPlantProject").expect("-8 MC, PROD[Energy]")
  }

  @Test
  internal fun `Does not award the rebate after selling patents`() {
    p1.runOperation("ProjectCard")

    p1.sellPatents(1).expect("-ProjectCard, MC")
  }

  @Test
  internal fun `Spire can cash in science through Standard Technology after temperature is maxed`() {
    newGame(PreludeExpansion, Prelude2CardPack, CorporateEraExpansion)
    p1.playCorp(Spire, 4)
    val p2 = requireP2()
    p2.runOperation("300 MC")
    admin.phase("Prelude")
    p1.playPrelude(DomeFarming)
    p1.playPrelude(IoResearchOutpost)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction")
    p1.playProject(Research, 11)
    p1.playProject(PowerPlant, 4)
    p1.playProject(BusinessContacts, 7)
    p1.playProject(Pets, 10)
    p1.playProject(StandardTechnology, 6)
    // The other player has finished the temperature track; there is no TR left to buy.
    repeat(19) {
      p2.stdProject("AsteroidProject") {
        if (p2.count("TemperatureStep") == 15) placeTile(1, 2)
      }
    }
    p1.count("Science<$Spire>") shouldBe 7

    p1.stdProject(
            "AsteroidProject",
            payment = {
              doTask("-7 Science<$Spire>")
              declineTask()
            },
        )
        .expect("3 MC, -7 Science<$Spire>, 0 TemperatureStep, 0 TerraformRating")
  }
}
