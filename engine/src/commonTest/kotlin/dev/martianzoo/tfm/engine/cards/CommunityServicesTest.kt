package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import kotlin.test.Test

class CommunityServicesTest : CardTest() {
  @Test
  fun `with three tagless cards, adds Community Services`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.manual("AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
    p1.manual("Airliners") { doTask("2 Floater<AtmoCollectors>") }
    p1.manual("PROD[2]")
    // Three tagless cards: Atmo Collectors, Airliners, and Community Services itself.
    p1.manual("CommunityServices").expect("PROD[3]")
  }
}
