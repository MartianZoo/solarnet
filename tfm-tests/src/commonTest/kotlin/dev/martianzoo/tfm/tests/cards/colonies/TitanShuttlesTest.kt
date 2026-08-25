package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class TitanShuttlesTest : ColoniesCardTest() {
  @Test
  internal fun `Can convert five floaters into five titanium`() {
    initializeCard()
    p1.cardAction2(TitanShuttles) {
          doTask("-5 Floater<$TitanShuttles> THEN 5 Titanium")
        }
        .expect("-5 Floater<$TitanShuttles>, 5 Titanium")
  }

  @Test
  internal fun `Cannot underpay its floater cost`() {
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
