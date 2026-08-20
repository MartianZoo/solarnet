package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class AdvertisingTest : CardTest() {
  @Test
  fun `with Advertising, adds cards costing twenty and nineteen`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("$Advertising")
    p1.manual("$LunarExports") { doTask("PROD[5]") }.expect("PROD[5]")
    p1.manual("$GanymedeColony").expect("PROD[1]")
  }
}
