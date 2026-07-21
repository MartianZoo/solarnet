package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class AntsTest : CardTest() {
  // FAQ: "consume an ant to produce an ant"
  @Test
  fun `Ants can eat its own microbe and trigger Topsoil Contract`() {
    val p1 = newGame("BMX", 2).tfm(PLAYER1)
    p1.sneak("Ants, TopsoilContract")
    p1.manual("SymbioticFungus")
    p1.phase("Action")

    p1.cardAction1("SymbioticFungus") { doTask("Microbe<Ants>") }
        .expect("Microbe<Ants>, Megacredit, ActionUsedMarker<SymbioticFungus>")
    p1.cardAction1("Ants").expect("Megacredit, ActionUsedMarker<Ants>")

    p1.assertCounts(1 to "Microbe<Ants>")
  }
}
