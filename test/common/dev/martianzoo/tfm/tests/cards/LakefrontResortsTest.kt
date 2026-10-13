package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class LakefrontResortsTest : TfmSandboxTest() {
  @Test
  internal fun `An opponent's ocean placement raises MC production`() {
    newTestGame(addOptions = "LakefrontResorts", kimCorporation = LakefrontResorts)

    stan
        .inTurn {
          doTasks(
              "UseStandardProject<AquiferProject>",
              "-18 MC",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
          )
          kim.selectTask("PROD[MC]")
          doTask("2 Steel")
        }
        .expect("PROD[MC<Kim>]")
  }

  @Test
  internal fun `Does not pay when an opponent places a tile adjacent to an ocean`() {
    newTestGame(addOptions = "LakefrontResorts", kimCorporation = LakefrontResorts)
    kim.exMachina("OceanTile<Tharsis_1_2>")

    stan
        .inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "-25 MC",
              "PROD[MC]",
              "CityTile<Tharsis_2_2>",
              "Steel",
              "2 MC",
          )
        }
        .expect("0 MC<Kim>")
  }

  @Test
  internal fun `Pays for each ocean adjacency`() {
    newTestGame(addOptions = "LakefrontResorts", kimCorporation = LakefrontResorts)
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_2_1>")
    kim.setToExMachina(25, "MC")

    // Two ordinary adjacency bonuses plus two Lakefront bonuses total 6 MC.
    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "-25 MC",
              "PROD[MC]",
              "CityTile<Tharsis_2_2>",
              "Steel",
          )
          selectTask("MC! / Neighbor<OceanTile, Tharsis_2_2>")
          doTasks("2 MC", "2 MC")
        }
        .expect("-19 MC")
  }
}
