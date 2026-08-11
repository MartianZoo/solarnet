package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class TradeTrackIncreaseTest : ColoniesCardTest() {
  @Test
  fun `Trade Envoys raises the track before trade income`() {
    p1.manual("ProjectCard, 15")
    p1.playProject("TradeEnvoys", 6)
    engine.manual("3 ColonyProduction<Luna>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      p1.assertCounts(1 to "TradeBarrier", 0 to "FlownTradeFleet")
      shouldThrow<NarrowingException> {
        doTask("FlownTradeFleet<Luna> FROM ReserveTradeFleet")
      }
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        0 to "ReserveTradeFleet",
        1 to "FlownTradeFleet<Luna>",
        0 to "ColonyProduction<Luna>",
        13 to "Megacredit",
    )
  }

  @Test
  fun `Trade Envoys raises the track when Titan Floating Launch-Pad trades`() {
    p1.manual("TradeEnvoys, TitanFloatingLaunchPad") {
      doTask("2 Floater<TitanFloatingLaunchPad>")
    }
    engine.manual("3 ColonyProduction<Luna>")

    p1.cardAction2("TitanFloatingLaunchPad") {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        1 to "Floater<TitanFloatingLaunchPad>",
        1 to "FlownTradeFleet<Luna>",
        13 to "Megacredit",
    )
  }

  @Test
  fun `Trade Envoys does not increase a maxed track`() {
    p1.manual("TradeEnvoys, 9")
    engine.manual("5 ColonyProduction<Luna>")
    p1.assertCounts(6 to "ColonyProduction<Luna>")

    p1.stdAction("TradeSA") { doTask("Trade<Luna>") }

    p1.assertCounts(
        1 to "FlownTradeFleet<Luna>",
        0 to "ColonyProduction<Luna>",
        17 to "Megacredit",
    )
  }

  @Test
  fun `may decline to increase track`() {
    p1.manual("ProjectCard, 30")
    p1.playProject("TradingColony", 18) {
      doTask("Colony<Europa>")
      doTask("OceanTile<Tharsis_1_2>")
    }
    engine.manual("ColonyProduction<Europa>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Europa>")
      doTask("Ok")
    }

    p1.assertCounts(
        0 to "ReserveTradeFleet",
        1 to "FlownTradeFleet<Europa>",
        1 to "ColonyProduction<Europa>",
    )
    p1.assertProds(1 to "Energy")
  }

  @Test
  fun `all track decisions precede the trade when both cards are active`() {
    p1.manual("2 ProjectCard, 50")
    p1.playProject("TradeEnvoys", 6)
    p1.playProject("TradingColony", 18) {
      doTask("Colony<Europa>")
      doTask("OceanTile<Tharsis_1_2>")
    }
    engine.manual("3 ColonyProduction<Luna>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      p1.assertCounts(2 to "TradeBarrier", 0 to "FlownTradeFleet")
      val decisions =
          tasks.matching { it.instruction.toString().startsWith("ColonyProduction<") }.toList()
      p1.reviseTask(decisions[0], "ColonyProduction<Luna>")
      if (decisions[0] in tasks) {
        p1.doTask("ColonyProduction<Luna>", tasks.ids().indexOf(decisions[0]) + 1)
      }
      shouldThrow<NarrowingException> {
        doTask("FlownTradeFleet<Luna> FROM ReserveTradeFleet")
      }
      p1.reviseTask(decisions[1], "Ok")
      if (decisions[1] in tasks) p1.doTask("Ok", tasks.ids().indexOf(decisions[1]) + 1)
    }

    p1.assertCounts(
        1 to "FlownTradeFleet<Luna>",
        0 to "ColonyProduction<Luna>",
    )
  }
}
