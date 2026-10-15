package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.TradingColony
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TradingColonyTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `May decline its track increase before trade income`() {
    kim.exMachina("$TradingColony, Colony<Europa>")
    kim.setToExMachina(9, "MC")
    admin.setToExMachina(2, "ColonyProduction<Europa>")

    kim.inTurn {
      doTasks("TradeAction", "9 Pay<> FROM MC")

      doTasks("Trade<Europa>")
      // Hand the colony-track change to Admin before collecting trade income.
      selectTask("ColonyProduction<Europa>?")
      doTasks("0 ColonyProduction<Europa>", "PROD[Energy]", "MC")
    }

    kim.assertCounts(1 to "ColonyProduction<Europa>")
  }
}
