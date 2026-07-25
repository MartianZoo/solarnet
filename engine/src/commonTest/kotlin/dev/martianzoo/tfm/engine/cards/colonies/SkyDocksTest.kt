package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class SkyDocksTest : ColoniesCardTest() {
  @Test
  fun `get an extra trade fleet`() {
    shouldThrow<RequirementException> { p1.playProject("SkyDocks", 18) }

    // These have to be played: Sky Docks requires their Earth tags.
    p1.playProject("Sponsors", 6)
    p1.playProject("MediaGroup", 6)

    p1.playProject("SkyDocks", 18).expect("ReserveTradeFleet")
    p1.assertCounts(2 to "TradeFleet", 2 to "ReserveTradeFleet")

    p1.stdAction("TradeSA", 1) { doTask("Trade<Luna>") }
    p1.stdAction("TradeSA", 1) { doTask("Trade<Triton>") }
    p1.assertCounts(2 to "TradeFleet", 2 to "FlownTradeFleet", 0 to "ReserveTradeFleet")
  }
}
