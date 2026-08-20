package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.cardnames.CorroderSuits
import kotlin.test.Test

class CorroderSuitsTest : CardTest() {
  @Test
  fun `without a compatible Venus card, resolves Corroder Suits`() {
    newGame(VenusNextExpansion)

    p1.manual("$CorroderSuits").expect("PROD[2], 0 CardResource")
  }
}
