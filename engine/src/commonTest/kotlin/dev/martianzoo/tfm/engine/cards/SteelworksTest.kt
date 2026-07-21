package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class SteelworksTest : CardTest() {
  // FAQ: "used even after the oxygen has been maxed out"
  @Test
  fun `Steelworks still produces steel after oxygen is maxed`() {
    val p1 = newGame("BM", 2).tfm(PLAYER1)
    p1.sneak("Steelworks, 4 Energy, 14 OxygenStep")
    p1.phase("Action")

    p1.cardAction1("Steelworks").expect("-4 Energy, 2 Steel, ActionUsedMarker<Steelworks>")

    p1.assertCounts(14 to "OxygenStep", 20 to "TerraformRating")
  }
}
