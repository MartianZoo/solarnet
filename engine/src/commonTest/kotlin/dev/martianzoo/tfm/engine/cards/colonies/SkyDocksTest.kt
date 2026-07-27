package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class SkyDocksTest : ColoniesCardTest() {
  @Test
  fun `with two Earth tags, plays Sky Docks`() {
    p1.manual("ProjectCard, 18, LunaGovernor")
    p1.playProject("SkyDocks", 18).expect("ReserveTradeFleet")
    p1.assertCounts(2 to "TradeFleet", 2 to "ReserveTradeFleet")
  }

  @Test
  fun `with Sky Docks, makes two trades`() {
    p1.manual("SkyDocks, 18")
    p1.stdAction("TradeSA", 1) { doTask("Trade<Luna>") }
    p1.stdAction("TradeSA", 1) { doTask("Trade<Triton>") }
    p1.assertCounts(2 to "TradeFleet", 2 to "FlownTradeFleet", 0 to "ReserveTradeFleet")
  }

  @Test
  fun `with one Earth tag, tries to play Sky Docks`() {
    p1.manual("ProjectCard, 18, HeavyTaxation")
    shouldThrow<RequirementException> { p1.playProject("SkyDocks", 18) }
  }
}
