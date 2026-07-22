package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class EnergyTappingTest : CardTest() {
  // FAQ: "raise your own energy production and then reduce it again"
  @Test
  fun `Energy Tapping can create and then remove Manutech's energy production`() {
    val p1 = newGame("BRMV", 2).tfm(PLAYER1)
    p1.sneak("Manutech")

    p1.manual("EnergyTapping").expect("Energy")

    p1.assertProds(0 to "Energy")
  }
}
