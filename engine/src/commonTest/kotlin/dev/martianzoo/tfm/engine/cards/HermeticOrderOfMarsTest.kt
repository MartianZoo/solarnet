package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon.Option.*
import kotlin.test.Test

class HermeticOrderOfMarsTest : CardTest() {
  @Test
  fun `gains money for each empty area adjacent to its own tiles`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual(
        "10, ProjectCard, CityTile<Tharsis_1_1>, CityTile<Tharsis_2_1>, " + "CityTile<Tharsis_2_2>"
    )
    requireP2().manual("OceanTile<Tharsis_1_2>, CityTile<Tharsis_3_2>")

    p1.playProject("HermeticOrderOfMars", 10).expect("PROD[2]")
  }
}
