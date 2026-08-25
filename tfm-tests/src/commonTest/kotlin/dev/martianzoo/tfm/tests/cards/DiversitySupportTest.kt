package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DiversitySupportTest : CardTest() {
  @Test
  internal fun `Can be played with nine resource types`() {
    seedResources()
    p1.manual("$ForcedPrecipitation, Floater<$ForcedPrecipitation>")
    p1.playProject(DiversitySupport, 1).expect("TerraformRating")
  }

  @Test
  internal fun `Cannot be played with only eight resource types`() {
    seedResources()
    p1.count("TerraformRating") shouldBe 20
    shouldThrow<RequirementException> { p1.playProject(DiversitySupport, 1) }
    p1.count("TerraformRating") shouldBe 20
  }

  private fun seedResources() {
    newGame(VenusNextExpansion, PromoCardPack)
    engine.phase("Action")
    requireP2()
        .manual(
            "10 Megacredit, 9 ProjectCard, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat, " +
                "$EarthCatapult, $Mine, $InventorsGuild"
        )
    p1.manual(
        "6 Megacredit, 5 ProjectCard, 4 Steel, 3 Titanium, 2 Plant, 2 Energy, 2 Heat, " +
            "$Pets, $Decomposers, Animal<$Pets>, Microbe<$Decomposers>"
    )
  }
}
