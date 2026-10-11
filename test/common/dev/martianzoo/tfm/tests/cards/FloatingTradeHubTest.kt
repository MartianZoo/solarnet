package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.FloatingTradeHub
import kotlin.test.Test

internal class FloatingTradeHubTest : TfmSandboxTest() {
  @Test
  internal fun `Converts only the chosen floaters into titanium with Prelude and Prelude 2`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack")
    advanceUntilActionPhase()
    kim.exMachina("$FloatingTradeHub, 5 Floater<$FloatingTradeHub>")

    kim.inTurn {
      doTask("UseCardAction<FloatingTradeHub, Action2>")
      selectOnlyTask()
      selectTask("-3 Floater<FloatingTradeHub>! THEN 3 StandardResource!")
      selectOnlyTask()
      doTask("3 Titanium")
    }
  }

  @Test
  internal fun `Converts only the chosen floaters into titanium without Prelude`() {
    newTestGame(addOptions = "-PreludeExpansion, -Prelude2CardPack, FloatingTradeHub")
    kim.exMachina("$FloatingTradeHub, 5 Floater<$FloatingTradeHub>")

    kim.inTurn {
      doTask("UseCardAction<FloatingTradeHub, Action2>")
      selectOnlyTask()
      selectTask("-3 Floater<FloatingTradeHub>! THEN 3 StandardResource!")
      selectOnlyTask()
      doTask("3 Titanium")
    }
  }
}
