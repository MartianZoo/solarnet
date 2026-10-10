package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TradeEnvoysTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Raises the track before trade income`() {
    kim.exMachina("$TradeEnvoys")
    kim.setToExMachina(9, "MC")
    admin.sneak("3 ColonyProduction<Luna>")

    kim.stdAction("TradeAction") {
          doTask("Trade<Luna>")
          doTask("ColonyProduction")
        }
        .expect("4 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun `Raises the track when Titan Floating Launch-Pad trades`() {
    kim.exMachina("$TradeEnvoys, $TitanFloatingLaunchPad, 2 Floater<$TitanFloatingLaunchPad>")
    kim.setToExMachina(0, "MC")
    admin.sneak("3 ColonyProduction<Luna>")

    kim.cardAction2(TitanFloatingLaunchPad) {
          doTask("Trade<Luna>")
          doTask("ColonyProduction")
        }
        .expect("-Floater<$TitanFloatingLaunchPad>, 13 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun `Does not increase a maxed track`() {
    kim.exMachina("$TradeEnvoys")
    kim.setToExMachina(9, "MC")
    admin.sneak("5 ColonyProduction<Luna>")

    kim.stdAction("TradeAction") { doTask("Trade<Luna>") }.expect("8 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun `Both track decisions precede trade income`() {
    kim.exMachina("$TradeEnvoys, $TradingColony")
    kim.setToExMachina(9, "MC")
    admin.sneak("3 ColonyProduction<Luna>")

    kim.stdAction("TradeAction") {
          doTask("Trade<Luna>")
          doTask("ColonyProduction")
          // Decline the other card's additional optional track increase.
          declineTask()
        }
        .expect("4 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }
}
