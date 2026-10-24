package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.FloatingTradeHub
import kotlin.test.Test

internal class FloatingTradeHubTest : TfmSandboxTest() {
  @Test
  internal fun `Converts only the chosen floaters into titanium without Prelude`() {
    newTestGame(addOptions = "FloatingTradeHub")
    kim.exMachina("$FloatingTradeHub, 5 Floater<$FloatingTradeHub>")

    kim.inTurn {
      doTasks(*cardActionTasks("FloatingTradeHub", "Action2"))
      doTask("-3 Floater<FloatingTradeHub> THEN 3 Titanium")
      doTask("3 Titanium")
    }
  }
}
