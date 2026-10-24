package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TradeEnvoysTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Raises the track before trade income`() {
    kim.exMachina("$TradeEnvoys")
    kim.setToExMachina(9, "MC")
    admin.setToExMachina(4, "ColonyProduction<Luna>")

    kim.inTurn {
          doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC")

          doTasks("Trade<Luna>")
          // Hand the colony-track change to Admin before collecting trade income.
          selectTask("ColonyProduction<Luna>?")
          // Hand the colony-track change to Admin before collecting trade income.
          selectTask("ColonyProduction<Luna>!")
          doTask("13 MC")
        }
        .expect("4 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun `Raises the track when Titan Floating Launch-Pad trades`() {
    kim.exMachina("$TradeEnvoys, $TitanFloatingLaunchPad, 2 Floater<$TitanFloatingLaunchPad>")
    kim.setToExMachina(0, "MC")
    admin.setToExMachina(4, "ColonyProduction<Luna>")

    kim.inTurn {
      doTasks(*cardActionTasks("TitanFloatingLaunchPad", "Action2"))
      doTask("-Floater<TitanFloatingLaunchPad>")

      doTasks("Trade<Luna>")
      // Hand the colony-track change to Admin before collecting trade income.
      selectTask("ColonyProduction<Luna>?")
      // Hand the colony-track change to Admin before collecting trade income.
      selectTask("ColonyProduction<Luna>!")
      doTask("13 MC")
    }

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun `Does not increase a maxed track`() {
    kim.exMachina("$TradeEnvoys")
    kim.setToExMachina(9, "MC")
    admin.setToExMachina(6, "ColonyProduction<Luna>")

    kim.inTurn {
          doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC")

          doTasks("Trade<Luna>")
          // Hand the colony-track change to Admin before collecting trade income.
          selectTask("ColonyProduction<Luna>?")
          doTask("17 MC")
        }
        .expect("8 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }

  @Test
  internal fun `Both track decisions precede trade income`() {
    kim.exMachina("$TradeEnvoys, $TradingColony")
    kim.setToExMachina(9, "MC")
    admin.setToExMachina(4, "ColonyProduction<Luna>")

    kim.inTurn {
          // Decline the other card's additional optional track increase.
          doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC")

          doTasks("Trade<Luna>")
          // Hand the colony-track change to Admin before collecting trade income.
          selectTask("ColonyProduction<Luna>?")
          // Hand the colony-track change to Admin before collecting trade income.
          selectTask("ColonyProduction<Luna>!")
          // Hand the colony-track change to Admin before collecting trade income.
          selectTask("ColonyProduction<Luna>?")
          doTasks("0 ColonyProduction<Luna>", "13 MC")
        }
        .expect("4 MC")

    kim.assertCounts(0 to "ColonyProduction<Luna>")
  }
}
