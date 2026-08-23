package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.cardnames.CorroderSuits
import kotlin.test.Test

internal class CorroderSuitsTest : CardTest() {
  @Test
  internal fun `Can be played without another compatible Venus card`() {
    newGame(VenusNextExpansion)

    p1.manual("$CorroderSuits").expect("PROD[2], 0 CardResource")
  }
}
