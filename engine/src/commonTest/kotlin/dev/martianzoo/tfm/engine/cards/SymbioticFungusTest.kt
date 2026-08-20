package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class SymbioticFungusTest : CardTest() {
  // FAQ: "the microbe obtained would be discarded"
  @Test
  fun `Can use its action without an eligible target`() {
    newGame()
    p1.manual("$SymbioticFungus")
    engine.phase("Action")
    p1.cardAction1(SymbioticFungus).expect("0 Microbe")
  }
}
