package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class SkyDocksTest : ColoniesCardTest() {
  @Test
  fun `get an extra trade fleet`() {
    shouldThrow<RequirementException> { p1.playProject("SkyDocks", 18) }

    // These have to be played: Sky Docks requires their Earth tags.
    p1.playProject("Sponsors", 6)
    p1.playProject("MediaGroup", 6)

    p1.playProject("SkyDocks", 18) {
          shouldThrow<LimitsException> { doTask("TradeFleetC") }
          doTask("TradeFleetD")
        }
        .expect("TradeFleetD")

    p1.stdAction("TradeSA", 1) {
      // Can't use a trade fleet we don't have
      shouldThrow<DependencyException> { doTask("Trade<Luna, TradeFleetE>") }
      doTask("Trade<Luna, TradeFleetA>")
    }
    p1.stdAction("TradeSA", 1) {
      doTask("Trade<Triton, TradeFleetD>")
    }
  }
}
