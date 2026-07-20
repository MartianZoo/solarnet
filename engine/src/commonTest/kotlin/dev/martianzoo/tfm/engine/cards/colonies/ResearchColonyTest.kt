package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ResearchColonyTest : ColoniesCardTest() {
  @Test
  fun `can put a colony where you already have one`() {
    p1.sneak("Colony<Luna>")
    shouldThrow<NarrowingException> { p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") } }
    p1.playProject("ResearchColony", 20) { doTask("Colony<Luna>") }.expect("-20, Colony<Luna>")
  }

  @Test
  fun `but still not over the max of three`() {
    p1.sneak("Colony<Luna>, 2 Colony<Player2, Luna>")
    shouldThrow<LimitsException> {
      p1.playProject("ResearchColony", 20) { doTask("Colony<Luna>") }
    }
  }
}
