package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class CommunityServicesTest : CardTest() {
  @Test
  fun communityServices() {
    val game = newBareGame(Canon.fromOptionCodes("CVERB", 2, testColonyTiles(2)))
    val p1 = game.tfm(PLAYER1)

    p1.sneak("10 ProjectCard, AtmoCollectors, Airliners, 4 Floater<AtmoCollectors>, PROD[2]")

    // Three tagless cards: Atmo Collectors, Airliners, and Community Services itself.
    p1.manual("CommunityServices").expect("PROD[3]")
  }
}
