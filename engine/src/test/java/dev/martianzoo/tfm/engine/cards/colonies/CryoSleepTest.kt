package dev.martianzoo.tfm.engine.cards.colonies

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CryoSleepTest : ColoniesCardTest() {
  @BeforeEach
  fun setUp() {
    p1.godMode().manual("CryoSleep")
  }

  @Test
  fun testMegacredit() {
    p1.stdAction("TradeSA", 1) {
      doTask("Trade<Io, TradeFleetA>") // TODO auto-choose
    }.expect("-8, 3 Heat")
  }

  @Test
  fun testEnergy() {
    p1.godMode().manual("2 Energy")
    p1.stdAction("TradeSA", 2) { doTask("Trade<Io, TradeFleetA>") }.expect("-2 Energy, 3 Heat")
  }

  @Test
  fun testTitanium() {
    p1.godMode().manual("2 Titanium")
    p1.stdAction("TradeSA", 3) { doTask("Trade<Io, TradeFleetA>") }.expect("-2 Titanium, 3 Heat")
  }

  @Test
  fun testWithRimFreightersToo() {
    p1.godMode().manual("RimFreighters")
    p1.stdAction("TradeSA", 1) { doTask("Trade<Io, TradeFleetA>") }.expect("-7, 3 Heat")
  }
}
