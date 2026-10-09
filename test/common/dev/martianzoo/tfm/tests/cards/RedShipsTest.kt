package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.RedShips
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RedShipsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Counts each city or special tile beside an ocean`() {
    kim.exMachina(
        "$RedShips, NormalCityTile<Kim, Tharsis_1_3>, OceanTile<Tharsis_1_2>, " +
            "MiningRights_SpecialTile<Kim, Tharsis_2_2>"
    )

    kim.cardAction1(RedShips).expect("2 MC")
  }
}
