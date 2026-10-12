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
    admin.sneak("ColonyProduction<Europa>")

    kim.inTurn {
      doTasks("TradeAction", "-MC / Owed<Class<MC>>")
      selectTask("Trade!")
      doTasks("Trade<Europa>", "LATER { FinishTrade<Europa> }")
      selectTask("ColonyProduction<Europa>?")
      doTasks("Ok", "PROD[Energy]", "MC")
    }

    kim.assertCounts(1 to "ColonyProduction<Europa>")
  }
}
