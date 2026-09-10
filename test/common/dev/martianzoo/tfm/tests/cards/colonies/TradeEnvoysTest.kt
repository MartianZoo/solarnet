package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TradeEnvoysTest : ColoniesCardTest() {
  @Test
  internal fun `Raises the track before trade income`() {
    p1.runOperation("ProjectCard, 15 MC")
    p1.playProject(TradeEnvoys, 6)
    admin.runOperation("3 ColonyProduction<Luna>")

    p1.stdAction("TradeAction") {
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
    p1.runOperation("$TradeEnvoys, $TitanFloatingLaunchPad") {
      addCardResources(TitanFloatingLaunchPad)
    }
    admin.runOperation("3 ColonyProduction<Luna>")

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
    p1.runOperation("$TradeEnvoys, 9 MC")
    admin.runOperation("5 ColonyProduction<Luna>")
    p1.assertCounts(6 to "ColonyProduction<Luna>")

    p1.stdAction("TradeAction") { doTask("Trade<Luna>") }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
        17 to "MC",
    )
  }

  @Test
  internal fun `Trading Colony may decline the shared track increase`() {
    p1.runOperation("ProjectCard, 30 MC")
    p1.playProject(TradingColony, 18) {
      doTask("Colony<Europa>")
      placeTile(1, 2)
    }
    admin.runOperation("ColonyProduction<Europa>")

    p1.stdAction("TradeAction") {
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
  internal fun `All track decisions precede the trade when both cards are active`() {
    p1.runOperation("2 ProjectCard, 50 MC")
    p1.playProject(TradeEnvoys, 6)
    p1.playProject(TradingColony, 18) {
      doTask("Colony<Europa>")
      placeTile(1, 2)
    }
    admin.runOperation("3 ColonyProduction<Luna>")

    p1.stdAction("TradeAction") {
      doTask("Trade<Luna>")
      doTask("ColonyProduction<Luna>")
      // Decline Trade Envoys' additional optional Luna colony-track increase.
      declineTask()
    }

    p1.assertCounts(
        0 to "ColonyProduction<Luna>",
    )
  }
}
