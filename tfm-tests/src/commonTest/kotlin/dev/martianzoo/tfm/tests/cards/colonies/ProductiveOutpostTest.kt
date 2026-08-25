package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ProductiveOutpostTest : ColoniesCardTest() {
  @Test
  internal fun `Pays no bonuses without colonies`() {
    p1.manual("$ProductiveOutpost").expect("0 Megacredit")
  }

  @Test
  internal fun `Pays each bonus for colonies the player owns`() {
    p1.manual("Colony<Luna>, Colony<Io>, Colony<Triton>")

    p1.manual("$ProductiveOutpost").expect("2, 2 Heat, Titanium")
  }
}
