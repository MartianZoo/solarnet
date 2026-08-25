package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Poseidon
import kotlin.test.Test

internal class PoseidonTest : CardTest() {
  @Test
  internal fun `Places its colony as the first action`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Poseidon, 0).expect("45")
    p1.assertCounts(1 to "Mandate", 0 to "Colony")

    engine.phase("Action")
    p1.stdAction("HandleMandates") { doTask("Colony<Luna>") }.expect("Colony<Luna>")
    p1.assertCounts(0 to "Mandate")
  }
}
