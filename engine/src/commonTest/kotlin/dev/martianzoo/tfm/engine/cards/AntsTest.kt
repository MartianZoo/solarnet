package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class AntsTest : CardTest() {
  // FAQ: "consume an ant to produce an ant"
  @Test
  fun `with a microbe on Ants, uses its action`() {
    newGame("PromoCardPack")
    p1.manual("Ants, TopsoilContract")
    p1.manual("SymbioticFungus")
    engine.phase("Action")

    p1.cardAction1("SymbioticFungus") { doTask("Microbe<Ants>") }
        .expect("Microbe<Ants>, Megacredit, ActionUsedMarker<SymbioticFungus>")
    p1.cardAction1("Ants").expect("Megacredit, ActionUsedMarker<Ants>")

    p1.assertCounts(1 to "Microbe<Ants>")
  }
}
