package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class CuttingEdgeTechnologyTest : CardTest() {
  @Test
  internal fun `Discounts cards with and without requirements`() {
    newGame(VenusNextExpansion, PromoCardPack)
    engine.phase("Action")
    p1.manual(
        "4, 2 ProjectCard, $CuttingEdgeTechnology, Steel, Titanium, Plant, Energy, Heat, " +
            "$Pets, $Decomposers, $ForcedPrecipitation, Animal<$Pets>, Microbe<$Decomposers>, " +
            "Floater<$ForcedPrecipitation>"
    )

    p1.playProject(DiversitySupport, 0).expect("TerraformRating")
    p1.playProject(Mine, 4)
  }
}
