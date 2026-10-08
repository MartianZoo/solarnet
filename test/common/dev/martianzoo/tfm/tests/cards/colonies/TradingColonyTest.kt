package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.TradingColony
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TradingColonyTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `May decline its track increase before trade income`() {
    kim.exMachina("$TradingColony, Colony<Europa>")
    kim.setToExMachina(9, "MC")
    kim.setToExMachina(2, "ColonyProduction<Europa>")
    val startingEnergyProduction = kim.production(cn("Energy"))

    kim.stdAction("TradeAction") {
      doTask("Trade<Europa>")
      declineTask()
    }

    kim.assertCounts(1 to "ColonyProduction<Europa>")
    kim.production(cn("Energy")) shouldBe startingEnergyProduction + 1
  }
}
