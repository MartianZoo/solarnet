package dev.martianzoo.tfm.engine.cards.colonies

import kotlin.test.Test

class ProductiveOutpostTest : ColoniesCardTest() {
  @Test
  fun `with no colonies, pays no bonuses`() {
    p1.manual("ProductiveOutpost").expect("0 Megacredit")
  }

  @Test
  fun `pays each bonus for colonies the player owns`() {
    p1.manual("Colony<Luna>, Colony<Io>, Colony<Triton>")

    p1.manual("ProductiveOutpost").expect("2, 2 Heat, Titanium")
  }
}
