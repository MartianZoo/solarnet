package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AdvertisingTest : CardTest() {
  @Test
  internal fun `Triggers on a 20-cost card but not a 19-cost card`() {
    newGameWithAutoWorkflow(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    playUntilFirstActionPhase()

    p1.turn {
      playProject(Advertising, 4)
      playProject(LunarExports, 19) { doTask("PROD[5 MC]") }.expect("PROD[5 MC]")
    }
    requireP2().pass()

    p1.playProject(GanymedeColony, 20).expect("PROD[1 MC]")
  }
}
