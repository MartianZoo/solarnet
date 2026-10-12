package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CryoSleepTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Discounts an MC-funded trade`() {
    kim.exMachina("$CryoSleep")

    kim.inTurn {
          doTasks("TradeAction<Action1>", "-MC / Owed<Class<MC>>")
          selectTask("Trade!")
          doTasks("Trade<Io>", "LATER { FinishTrade<Io> }", "3 Heat")
        }
        .expect("-8 MC, 3 Heat")
  }

  @Test
  internal fun `Can fund a discounted trade with energy`() {
    kim.exMachina("$CryoSleep")
    kim.setToExMachina(2, "Energy")

    kim.inTurn {
      doTasks(
          "TradeAction<Action2>",
          "-2 Energy",
          "Trade<Io>",
          "LATER { FinishTrade<Io> }",
          "3 Heat",
      )
    }
  }

  @Test
  internal fun `Stacks its trade discount with Rim Freighters`() {
    kim.exMachina("$CryoSleep, $RimFreighters")

    kim.inTurn {
          doTasks("TradeAction<Action1>", "-MC / Owed<Class<MC>>")
          selectTask("Trade!")
          doTasks("Trade<Io>", "LATER { FinishTrade<Io> }", "3 Heat")
        }
        .expect("-7 MC, 3 Heat")
  }
}
