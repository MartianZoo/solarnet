package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.LimitsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// This isn't really a test specific to this card, just testing task reordering
class MinorityRefugeTest : ColoniesCardTest() {
  @Test
  fun `if too low on mc prod forced to pick Luna`() {
    p1.godMode().manual("PROD[-5]")
    p1.playProject("MinorityRefuge", 5) {
      assertThrows<LimitsException>("Io") { doTask("Colony<Io>") }
      assertThrows<LimitsException>("Triton") { doTask("Colony<Triton>") }
      doTask("Colony<Luna>")
    }
  }
}
