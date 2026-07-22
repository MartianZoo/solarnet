package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class MartianRailsTest : CardTest() {
  // FAQ: "even if there are NO cities on Mars (earning you 0 M€)."
  @Test
  fun `Martian Rails can spend energy when only an off-Mars city exists`() {
    val p1 = newGame("BM", 2).tfm(PLAYER1)
    p1.sneak("MartianRails, Energy")
    p1.manual("GanymedeColony").expect("CityTile<Area081>")
    p1.phase("Action")

    p1.cardAction1("MartianRails").expect("-Energy, ActionUsedMarker<MartianRails>")

    p1.assertCounts(0 to "Megacredit")
  }
}
