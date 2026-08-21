package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class ProductiveOutpostTest : ColoniesCardTest() {
  @Test
  fun `Pays no bonuses without colonies`() {
    p1.manual("$ProductiveOutpost").expect("0 Megacredit")
  }

  @Test
  fun `Pays each bonus for colonies the player owns`() {
    p1.manual("Colony<Luna>, Colony<Io>, Colony<Triton>")

    p1.manual("$ProductiveOutpost").expect("2, 2 Heat, Titanium")
  }
}
