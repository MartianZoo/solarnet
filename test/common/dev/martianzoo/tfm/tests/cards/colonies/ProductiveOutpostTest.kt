package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ProductiveOutpostTest : ColoniesCardTest() {
  @Test
  internal fun `Pays no bonuses without colonies`() {
    p1.manual("$ProductiveOutpost").expect("0 MC")
  }

  @Test
  internal fun `Pays each bonus for colonies the player owns`() {
    p1.manual("Colony<Luna>, Colony<Io>, Colony<Triton>")

    p1.manual("$ProductiveOutpost").expect("2 MC, 2 Heat, Titanium")
  }

  @Test
  internal fun `Pays once per colony, not once per colony tile`() {
    p1.manual("2 Colony<Luna>")

    p1.manual("$ProductiveOutpost").expect("4 MC")
  }
}
