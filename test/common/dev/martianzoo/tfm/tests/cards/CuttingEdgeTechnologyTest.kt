package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CuttingEdgeTechnologyTest : ProjectCardTest() {
  @Test
  internal fun `Discounts a card with a requirement`() {
    kim.exMachina(
        "Steel, Titanium, Plant, Energy, Heat, $CuttingEdgeTechnology, $Pets, $Decomposers, " +
            "$ForcedPrecipitation, Animal<$Pets>, Microbe<$Decomposers>, " +
            "Floater<$ForcedPrecipitation>"
    )

    kim.playProject(DiversitySupport, 0).expect("TerraformRating")
  }

  @Test
  internal fun `Does not discount a card without a requirement`() {
    kim.exMachina("$CuttingEdgeTechnology")

    kim.playProject(Advertising, 4).expect("-4 MC")
  }
}
