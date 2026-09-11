package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RecyclonTest : CardTest() {
  @Test
  internal fun `Adds a microbe when it enters play`() {
    newGame(PromoCardPack)
    p1.runOperation("$Recyclon").expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Gains a microbe when its owner plays a building card`() {
    newGame(PromoCardPack)
    p1.runOperation("$Recyclon")
    p1.runOperation("$Mine").expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Converts its accumulated microbes into plant production`() {
    newGame(PromoCardPack)
    p1.runOperation("$Recyclon")
    p1.runOperation("2 Microbe<$Recyclon>")

    p1.runOperation("$TitaniumMine") { doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]") }
        .expect("-2 Microbe, PROD[Plant]")
  }
}
