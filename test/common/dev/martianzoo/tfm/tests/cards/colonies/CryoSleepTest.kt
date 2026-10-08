package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CryoSleepTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Discounts a mc-funded trade`() {
    kim.exMachina("$CryoSleep")

    kim.stdAction("TradeAction", 1) { doTask("Trade<Io>") }.expect("-8 MC, 3 Heat")
  }

  @Test
  internal fun `Can fund a discounted trade with energy`() {
    kim.exMachina("$CryoSleep")
    kim.setToExMachina(2, "Energy")

    kim.stdAction("TradeAction", 2) { doTask("Trade<Io>") }.expect("-2 Energy, 3 Heat")
  }

  @Test
  internal fun `Stacks its trade discount with Rim Freighters`() {
    kim.exMachina("$CryoSleep, $RimFreighters")

    kim.stdAction("TradeAction", 1) { doTask("Trade<Io>") }.expect("-7 MC, 3 Heat")
  }
}
