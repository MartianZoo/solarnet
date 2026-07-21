package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class SymbioticFungusTest : CardTest() {
  // FAQ: "the microbe obtained would be discarded"
  @Test
  fun `Symbiotic Fungus can discard a microbe when no card can hold it`() {
    val p1 = newGame("BM", 2).tfm(PLAYER1)
    p1.sneak("SymbioticFungus")
    p1.phase("Action")

    p1.cardAction1("SymbioticFungus").expect("ActionUsedMarker<SymbioticFungus>")

    p1.assertCounts(0 to "Microbe")
  }
}
