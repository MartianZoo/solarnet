package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class HermeticOrderOfMarsTest : CardTest() {
  @Test
  internal fun `Gains money for each empty area adjacent to its own tiles`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual(
        "10 MC, ProjectCard, CityTile<Tharsis_1_1>, CityTile<Tharsis_2_1>, " +
            "CityTile<Tharsis_2_2>"
    )
    requireP2().manual("OceanTile<Tharsis_1_2>, CityTile<Tharsis_3_2>")

    p1.playProject(HermeticOrderOfMars, 10).expect("PROD[2 MC]")
  }
}
