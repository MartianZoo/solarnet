package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.FakeAridor
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import kotlin.test.Test

internal class FakeAridorTest : CardTest() {
  @Test
  internal fun `mandate adds one selected colony tile`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(FakeAridor, 0).expect("40")
    p1.assertCounts(1 to "Mandate")

    engine.phase("Action")
    p1.stdAction("HandleMandates") { doTask("Europa") }.expect("Europa, ColonyProduction")
    p1.assertCounts(0 to "Mandate")
  }

  @Test
  internal fun `delayed selection enters play immediately when its resource card already exists`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(FakeAridor, 0)
    p1.manual("$TitanShuttles")
    p1.manual("Floater<$TitanShuttles>")

    engine.phase("Action")
    p1.stdAction("HandleMandates") { doTask("DelayedTitan") }.expect("Titan, ColonyProduction")
    engine.assertCounts(1 to "Titan", 0 to "DelayedTitan")
  }
}
