package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AdvertisingTest : CardTest() {
  @Test
  internal fun `Triggers on a 20-cost card but not a 19-cost card`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("$Advertising")
    p1.manual("$LunarExports") { doTask("PROD[5 MC]") }.expect("PROD[5 MC]")
    p1.manual("$GanymedeColony").expect("PROD[1 MC]")
  }
}
