package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class RecyclonTest : CardTest() {
  @Test
  fun `when Recyclon enters play, adds a microbe`() {
    newGame(PromoCardPack)
    p1.manual("$Recyclon").expect("Microbe<$Recyclon>")
  }

  @Test
  fun `with Recyclon, adds a building card`() {
    newGame(PromoCardPack)
    p1.manual("$Recyclon")
    p1.manual("$Mine").expect("Microbe<$Recyclon>")
  }

  @Test
  fun `with three microbes on Recyclon, adds a building card`() {
    newGame(PromoCardPack)
    p1.manual("$Recyclon")
    p1.manual("2 Microbe<$Recyclon>")

    p1.manual("$TitaniumMine") {
          doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
        }
        .expect("-2 Microbe, PROD[Plant]")
  }
}
