package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.LimitsException
import kotlin.test.Test
import io.kotest.assertions.throwables.shouldThrow

// This isn't really a test specific to this card, just testing task reordering
class MinorityRefugeTest : ColoniesCardTest() {
  @Test
  fun `if too low on mc prod forced to pick Luna`() {
    p1.godMode().manual("PROD[-5]")
    p1.playProject("MinorityRefuge", 5) {
      shouldThrow<LimitsException> { doTask("Colony<Io>") }
      shouldThrow<LimitsException> { doTask("Colony<Triton>") }
      doTask("Colony<Luna>")
    }
  }
}
