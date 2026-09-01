package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TradeEnvoysTest : ColoniesCardTest() {
  @Test
  internal fun `Raises the track before trade income`() {
    p1.manual("ProjectCard, 15 MC")
    p1.playProject(TradeEnvoys, 6)
    engine.manual("3 ColonyProduction<Luna>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
        13 to "MC",
    )
  }

  @Test
  internal fun `Raises the track when Titan Floating Launch-Pad trades`() {
    p1.manual("$TradeEnvoys, $TitanFloatingLaunchPad") { addCardResources(TitanFloatingLaunchPad) }
    engine.manual("3 ColonyProduction<Luna>")

    p1.cardAction2(TitanFloatingLaunchPad) {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
    }

    p1.assertCounts(
        1 to "Floater<$TitanFloatingLaunchPad>",
        13 to "MC",
    )
  }

  @Test
  internal fun `Does not increase a maxed track`() {
    p1.manual("$TradeEnvoys, 9 MC")
    engine.manual("5 ColonyProduction<Luna>")
    p1.assertCounts(6 to "ColonyProduction<Luna>")

    p1.stdAction("TradeSA") { doTask("Trade<Luna>") }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
        17 to "MC",
    )
  }

  @Test
  internal fun `Trading Colony may decline the shared track increase`() {
    p1.manual("ProjectCard, 30 MC")
    p1.playProject(TradingColony, 18) {
      doTask("Colony<Europa>")
      placeTile(1, 2)
    }
    engine.manual("ColonyProduction<Europa>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Europa>")
      // Decline Trade Envoys' optional Europa colony-track increase.
      declineTask()
    }

    p1.assertCounts(
        1 to "ColonyProduction<Europa>",
    )
    p1.assertProds(1 to "Energy")
  }

  @Test
  internal fun `All track decisions precede fleet movement when both cards are active`() {
    p1.manual("2 ProjectCard, 50 MC")
    p1.playProject(TradeEnvoys, 6)
    p1.playProject(TradingColony, 18) {
      doTask("Colony<Europa>")
      placeTile(1, 2)
    }
    engine.manual("3 ColonyProduction<Luna>")

    p1.stdAction("TradeSA") {
      doTask("Trade<Luna>")
      p1.count("CompletedTrade<Luna>") shouldBe 0
      p1.count("AvailableTradeFleet") shouldBe 0
      p1.count("Trade<Luna>") shouldBe 1
      p1.count("TradeFleet") shouldBe 1
      doTask("ColonyProduction<Luna>")
      p1.count("CompletedTrade<Luna>") shouldBe 0
      // Decline Trade Envoys' additional optional Luna colony-track increase.
      declineTask()
      p1.count("CompletedTrade<Luna>") shouldBe 0
    }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
        0 to "Trade",
        1 to "CompletedTrade<Luna>",
        1 to "TradeFleet",
    )
  }
}
