package dev.martianzoo.tfm.engine.cards.colonies

import kotlin.test.Test

class TitanFloatingLaunchPadTest : ColoniesCardTest() {
  @Test
  fun `can trade for free`() {
    p1.sneak("TitanFloatingLaunchPad, 2 Floater<TitanFloatingLaunchPad>")
    p1.cardAction2("TitanFloatingLaunchPad") {
          doTask("Trade<Io, TradeFleetA>")
        }
        .expect("-Floater, 3 Heat")
  }
}
