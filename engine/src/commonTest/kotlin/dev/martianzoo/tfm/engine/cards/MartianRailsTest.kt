package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class MartianRailsTest : CardTest() {
  // FAQ: "even if there are NO cities on Mars (earning you 0 M€)."
  @Test
  fun `with only an off-Mars city, uses Martian Rails`() {
    newGame()
    p1.manual("$MartianRails, Energy")
    p1.manual("$GanymedeColony").expect("CityTile<Card081_RemoteArea>")
    engine.phase("Action")
    p1.cardAction1(MartianRails).expect("-Energy, 0 Megacredit")
    p1.assertCounts(0 to "Megacredit")
  }
}
