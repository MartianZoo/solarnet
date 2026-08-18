package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class TradeEnvoysTest : ColoniesCardTest() {
  @Test
  fun `raises the track before trade income`() {
    p1.manual("ProjectCard, 15")
    p1.playProject(TradeEnvoys, 6)
    engine.manual("3 ColonyProduction<Luna>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
        13 to "Megacredit",
    )
  }

  @Test
  fun `raises the track when Titan Floating Launch-Pad trades`() {
    p1.manual("$TradeEnvoys, $TitanFloatingLaunchPad") {
      doTask("2 Floater<$TitanFloatingLaunchPad>")
    }
    engine.manual("3 ColonyProduction<Luna>")

    p1.cardAction2(TitanFloatingLaunchPad) {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        1 to "Floater<$TitanFloatingLaunchPad>",
        13 to "Megacredit",
    )
  }

  @Test
  fun `does not increase a maxed track`() {
    p1.manual("$TradeEnvoys, 9")
    engine.manual("5 ColonyProduction<Luna>")
    p1.assertCounts(6 to "ColonyProduction<Luna>")

    p1.stdAction("TradeSA") { doTask("Trade<Luna>") }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
        17 to "Megacredit",
    )
  }

  @Test
  fun `Trading Colony may decline the shared track increase`() {
    p1.manual("ProjectCard, 30")
    p1.playProject(TradingColony, 18) {
      doTask("Colony<Europa>")
      doTask("OceanTile<Tharsis_1_2>")
    }
    engine.manual("ColonyProduction<Europa>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Europa>")
      doTask("Ok")
    }

    p1.assertCounts(
        1 to "ColonyProduction<Europa>",
    )
    p1.assertProds(1 to "Energy")
  }

  @Test
  fun `all track decisions precede the trade when both cards are active`() {
    p1.manual("2 ProjectCard, 50")
    p1.playProject(TradeEnvoys, 6)
    p1.playProject(TradingColony, 18) {
      doTask("Colony<Europa>")
      doTask("OceanTile<Tharsis_1_2>")
    }
    engine.manual("3 ColonyProduction<Luna>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
      doTask("Ok")
    }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
    )
  }
}
