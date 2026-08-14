package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class LakefrontResortsTest : CardTest() {
  @Test
  fun `with Lakefront Resorts and an ocean, p1 places an adjacent tile`() {
    newGame(TurmoilCardPack)
    val p2 = requireP2()

    engine.phase("Action")
    p1.manual("LakefrontResorts, 54")
    p2.manual("OceanTile<Tharsis_1_2>").expect("PROD[1]")

    // Two is the normal ocean-adjacency bonus; the third is Lakefront Resorts' bonus.
    p1.manual("CityTile<Tharsis_2_2>").expect("3")
  }

  @Test
  fun `with Lakefront Resorts owned by p2, p1 places an adjacent tile`() {
    newGame(TurmoilCardPack)
    val p2 = requireP2()
    engine.phase("Action")
    p2.manual("LakefrontResorts, 54")
    p1.manual("OceanTile<Tharsis_1_2>").expect("PROD[1]")
    p1.manual("CityTile<Tharsis_2_2>").expect("2")
  }
}
