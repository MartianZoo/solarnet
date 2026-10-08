package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class HermeticOrderOfMarsTest : ProjectCardTest() {
  @Test
  internal fun `Treats an area with a community but no tile as empty`() {
    kim.exMachina("NormalCityTile<Kim, Tharsis_1_1>")
    stan.exMachina(
        "OceanTile<Tharsis_1_2>, Community<Tharsis_2_1>, " + "NormalCityTile<Stan, Tharsis_2_2>"
    )

    kim.playProject(HermeticOrderOfMars, 10).expect("PROD[2 MC], -9 MC")
  }
}
