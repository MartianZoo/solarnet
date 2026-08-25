package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RecyclonTest : CardTest() {
  @Test
  internal fun `Adds a microbe when it enters play`() {
    newGame(PromoCardPack)
    p1.manual("$Recyclon").expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Gains a microbe when its owner plays a building card`() {
    newGame(PromoCardPack)
    p1.manual("$Recyclon")
    p1.manual("$Mine").expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Converts its accumulated microbes into plant production`() {
    newGame(PromoCardPack)
    p1.manual("$Recyclon")
    p1.manual("2 Microbe<$Recyclon>")

    p1.manual("$TitaniumMine") {
          doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
        }
        .expect("-2 Microbe, PROD[Plant]")
  }
}
