package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FloatingTradeHub
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class FloatingTradeHubTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion, Prelude2CardPack)
    admin.phase("Action")
    p1.runOperation("$FloatingTradeHub, 5 Floater<$FloatingTradeHub>")
  }

  @Test
  internal fun `First action adds two floaters`() {
    p1.cardAction1(FloatingTradeHub).expect("2 Floater<$FloatingTradeHub>")
  }

  @Test
  internal fun `Second action converts a chosen number of floaters into titanium`() {
    p1.cardAction2(FloatingTradeHub, x = 3) { doTask("3 Titanium") }
        .expect("-3 Floater<$FloatingTradeHub>, 3 Titanium")
  }
}
