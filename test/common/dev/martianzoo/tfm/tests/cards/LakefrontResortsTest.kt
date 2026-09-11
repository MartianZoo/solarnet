package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class LakefrontResortsTest : CardTest() {
  @Test
  internal fun `Pays when its owner places a tile adjacent to an ocean`() {
    newGame(TurmoilCardPack)
    val p2 = requireP2()

    admin.phase("Action")
    p1.runOperation("$LakefrontResorts, 54 MC")
    p2.runOperation("OceanTile<Tharsis_1_2>").expect("PROD[MC<Player1>]")

    // Two is the normal ocean-adjacency bonus; the third is Lakefront Resorts' bonus.
    p1.runOperation("CityTile<Tharsis_2_2>").expect("3 MC")
  }

  @Test
  internal fun `Does not pay when an opponent places a tile adjacent to an ocean`() {
    newGame(TurmoilCardPack)
    val p2 = requireP2()
    admin.phase("Action")
    p2.runOperation("$LakefrontResorts, 54 MC")
    p1.runOperation("OceanTile<Tharsis_1_2>").expect("PROD[MC<Player2>]")
    p1.runOperation("CityTile<Tharsis_2_2>").expect("2 MC")
  }

  @Test
  internal fun `Pays once for each ocean adjacency`() {
    newGame(TurmoilCardPack)
    admin.phase("Action")
    p1.runOperation("$LakefrontResorts, 54 MC")
    p1.runOperation("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_2_1>")

    // Four is the ordinary bonus for two oceans; Lakefront adds one per adjacency.
    p1.runOperation("CityTile<Tharsis_2_2>").expect("6 MC")
  }
}
