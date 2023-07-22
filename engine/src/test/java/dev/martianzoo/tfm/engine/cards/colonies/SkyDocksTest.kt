package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SkyDocksTest : ColoniesCardTest() {
  @Test
  fun `get an extra trade fleet`() {
    assertThrows<RequirementException> { p1.playProject("SkyDocks", 18) }

    p1.playProject("Sponsors", 6)
    p1.playProject("MediaGroup", 6)

    p1.playProject("SkyDocks", 18) {
      assertThrows<LimitsException> { doTask("TradeFleetC") }
      doTask("TradeFleetD")
    }

    p1.stdAction("TradeSA", 1) {
      // Can't use a trade fleet we don't have
      assertThrows<DependencyException> { doTask("Trade<Luna, TradeFleetE>") }
      doTask("Trade<Luna, TradeFleetA>")
    }
    p1.stdAction("TradeSA", 1) {
      // Why isn't this failing?? #62
      // assertThrows<LimitsException> { doTask("Trade<Triton, TradeFleetA>") }
      doTask("Trade<Triton, TradeFleetD>")
    }
  }
}
