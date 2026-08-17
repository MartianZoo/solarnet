package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class TitanShuttlesTest : ColoniesCardTest() {
  @Test
  fun `with seven floaters, uses Titan Shuttles`() {
    initializeCard()
    p1.cardAction2(TitanShuttles) {
          doTask("-5 Floater<$TitanShuttles> THEN 5 Titanium")
        }
        .expect("-5 Floater<$TitanShuttles>, 5 Titanium")
  }

  @Test
  fun `with seven floaters, tries an underpayment using Titan Shuttles`() {
    initializeCard()

    p1.cardAction2(TitanShuttles) {
      shouldThrow<NarrowingException> {
        doTask("-4 Floater<$TitanShuttles> THEN 5 Titanium")
      }
      abort()
    }
  }

  private fun initializeCard() {
    p1.manual("$TitanShuttles, 7 Floater<$TitanShuttles>")
  }
}
