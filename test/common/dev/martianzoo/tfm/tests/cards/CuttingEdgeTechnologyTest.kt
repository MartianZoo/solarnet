package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CuttingEdgeTechnologyTest : CardTest() {
  @Test
  internal fun `Discounts cards with and without requirements`() {
    newGame(VenusNextExpansion, PromoCardPack)
    admin.phase("Action")
    p1.runOperation(
        "4 MC, 2 ProjectCard, $CuttingEdgeTechnology, Steel, Titanium, Plant, Energy, Heat, " +
            "$Pets, $Decomposers, $ForcedPrecipitation, Animal<$Pets>, Microbe<$Decomposers>, " +
            "Floater<$ForcedPrecipitation>"
    )

    p1.playProject(DiversitySupport, 0).expect("TerraformRating")
    p1.playProject(Mine, 4)
  }
}
