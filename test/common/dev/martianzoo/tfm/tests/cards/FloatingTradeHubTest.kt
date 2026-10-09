package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.FloatingTradeHub
import kotlin.test.Test

internal class FloatingTradeHubTest : ProjectCardTest() {
  @Test
  internal fun `Converts only the chosen floaters into titanium with Prelude and Prelude 2`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack")
    kim.exMachina("$FloatingTradeHub, 5 Floater<$FloatingTradeHub>")

    kim.cardAction2(FloatingTradeHub, x = 3) { doTask("3 Titanium") }
        .expect("-3 Floater<$FloatingTradeHub>, 3 Titanium")
  }

  @Test
  internal fun `Converts only the chosen floaters into titanium without Prelude`() {
    newTestGame(addOptions = "-PreludeExpansion, -Prelude2CardPack, FloatingTradeHub")
    kim.exMachina("$FloatingTradeHub, 5 Floater<$FloatingTradeHub>")

    kim.cardAction2(FloatingTradeHub, x = 3) { doTask("3 Titanium") }
        .expect("-3 Floater<$FloatingTradeHub>, 3 Titanium")
  }
}
