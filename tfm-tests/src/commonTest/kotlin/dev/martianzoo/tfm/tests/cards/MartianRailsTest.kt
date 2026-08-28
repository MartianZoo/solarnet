package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MartianRailsTest : CardTest() {
  // FAQ: "even if there are NO cities on Mars (earning you 0 M€)."
  @Test
  internal fun `Can be used when every city is off Mars`() {
    newGame()
    p1.manual("$MartianRails, Energy")
    p1.manual("$GanymedeColony").expect("CityTile<GanymedeColony_RemoteArea>")
    engine.phase("Action")
    p1.cardAction1(MartianRails).expect("-Energy, 0 MC")
    p1.assertCounts(0 to "MC")
  }
}
