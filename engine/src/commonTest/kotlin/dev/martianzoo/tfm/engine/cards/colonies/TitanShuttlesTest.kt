package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.NarrowingException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class TitanShuttlesTest : ColoniesCardTest() {
  @Test
  fun `converts any chosen number of its floaters to titanium`() {
    p1.sneak("TitanShuttles, 7 Floater<TitanShuttles>")

    p1.cardAction2("TitanShuttles") {
          shouldThrow<NarrowingException> {
            doTask("-4 Floater<TitanShuttles> THEN 5 Titanium")
          }
          doTask("-5 Floater<TitanShuttles> THEN 5 Titanium")
        }
        .expect("-5 Floater<TitanShuttles>, 5 Titanium")
  }
}
