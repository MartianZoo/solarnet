package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class LakefrontResortsTest : TfmSandboxTest() {
  @Test
  internal fun `An opponent's ocean placement raises MC production`() {
    newTestGame(kimCorporation = LakefrontResorts)

    stan.stdProject("AquiferProject") { placeTile(1, 2) }.expect("PROD[MC<Kim>]")
  }

  @Test
  internal fun `Does not pay when an opponent places a tile adjacent to an ocean`() {
    newTestGame(kimCorporation = LakefrontResorts)
    kim.exMachina("OceanTile<Tharsis_1_2>")

    stan.stdProject("CityProject") { placeTile(2, 2) }.expect("-23 MC<Stan>, 0 MC<Kim>")
  }

  @Test
  internal fun `Pays for each ocean adjacency`() {
    newTestGame(kimCorporation = LakefrontResorts)
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_2_1>")
    kim.setToExMachina(25, "MC")

    // Two ordinary adjacency bonuses plus two Lakefront bonuses total 6 MC.
    kim.stdProject("CityProject") { placeTile(2, 2) }.expect("-19 MC")
  }
}
