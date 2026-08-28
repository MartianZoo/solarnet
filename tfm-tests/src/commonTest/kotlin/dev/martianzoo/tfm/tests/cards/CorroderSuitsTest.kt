package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.CorroderSuits
import kotlin.test.Test

internal class CorroderSuitsTest : CardTest() {
  @Test
  internal fun `Can be played without another compatible Venus card`() {
    newGame(VenusNextExpansion)

    p1.manual("$CorroderSuits").expect("PROD[2 MC], 0 CardResource")
  }
}
