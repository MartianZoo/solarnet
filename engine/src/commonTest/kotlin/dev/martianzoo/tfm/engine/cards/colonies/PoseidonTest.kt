package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.cards.CardTest
import dev.martianzoo.tfm.engine.cardnames.Poseidon
import kotlin.test.Test

class PoseidonTest : CardTest() {
  @Test
  fun `places its colony as the first action`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Poseidon, 0).expect("45")
    p1.assertCounts(1 to "Mandate", 0 to "Colony")

    engine.phase("Action")
    p1.stdAction("HandleMandates") { doTask("Colony<Luna>") }.expect("Colony<Luna>")
    p1.assertCounts(0 to "Mandate")
  }
}
