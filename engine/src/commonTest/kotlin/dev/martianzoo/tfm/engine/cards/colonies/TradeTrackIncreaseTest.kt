package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class TradeTrackIncreaseTest : ColoniesCardTest() {
  @Test
  fun `trade envoys offers a production track increase when trading`() {
    p1.playProject("TradeEnvoys", 6)

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        0 to "ReserveTradeFleet",
        1 to "FlownTradeFleet<Luna>",
        1 to "ColonyProduction<Luna>",
    )
  }
}
