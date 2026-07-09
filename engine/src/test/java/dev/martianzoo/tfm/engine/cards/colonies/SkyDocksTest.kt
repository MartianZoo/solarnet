package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import kotlin.test.Test
import io.kotest.assertions.throwables.shouldThrow

class SkyDocksTest : ColoniesCardTest() {
  @Test
  fun `get an extra trade fleet`() {
    shouldThrow<RequirementException> { p1.playProject("SkyDocks", 18) }

    p1.playProject("Sponsors", 6)
    p1.playProject("MediaGroup", 6)

    p1.playProject("SkyDocks", 18) {
      shouldThrow<LimitsException> { doTask("TradeFleetC") }
      doTask("TradeFleetD")
    }

    p1.stdAction("TradeSA", 1) {
      // Can't use a trade fleet we don't have
      shouldThrow<DependencyException> { doTask("Trade<Luna, TradeFleetE>") }
      doTask("Trade<Luna, TradeFleetA>")
    }
    p1.stdAction("TradeSA", 1) {
      // Why isn't this failing?? #62
      // shouldThrow<LimitsException> { doTask("Trade<Triton, TradeFleetA>") }
      doTask("Trade<Triton, TradeFleetD>")
    }
  }
}
