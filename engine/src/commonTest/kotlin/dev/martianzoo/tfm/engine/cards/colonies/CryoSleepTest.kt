package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class CryoSleepTest : ColoniesCardTest() {
  @Test
  fun `Discounts a megacredit-funded trade`() {
    p1.manual("$CryoSleep, 8")
    p1.stdAction("TradeSA", 1) { doTask("Trade<Io>") }.expect("-8, 3 Heat")
  }

  @Test
  fun `Can fund a trade with energy`() {
    p1.manual("$CryoSleep, 2 Energy")
    p1.stdAction("TradeSA", 2) { doTask("Trade<Io>") }.expect("-2 Energy, 3 Heat")
  }

  @Test
  fun `Can fund a trade with titanium`() {
    p1.manual("$CryoSleep, 2 Titanium")
    p1.stdAction("TradeSA", 3) { doTask("Trade<Io>") }.expect("-2 Titanium, 3 Heat")
  }

  @Test
  fun `Stacks its trade discount with Rim Freighters`() {
    p1.manual("$CryoSleep, $RimFreighters, 7")
    p1.stdAction("TradeSA", 1) { doTask("Trade<Io>") }.expect("-7, 3 Heat")
  }
}
