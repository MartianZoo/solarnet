package dev.martianzoo.tfm.engine.cards.colonies

import kotlin.test.Test

class ProductiveOutpostTest : ColoniesCardTest() {
  @Test
  fun `with no colonies, pays no bonuses`() {
    p1.manual("ProductiveOutpost").expect("0 Megacredit")
  }

  @Test
  fun `pays each bonus for colonies the player owns`() {
    p1.manual("2 Colony<Luna>, Colony<Io>")

    p1.manual("ProductiveOutpost").expect("4 Megacredit, 2 Heat")
  }
}
