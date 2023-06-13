package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ResearchColonyTest : ColoniesCardTest() {
  @Test
  fun `can put a colony where you already have one`() {
    p1.godMode().manual("Colony<Luna>")
    assertThrows<NarrowingException> {
      p1.stdProject("BuildColonySP") {
        doTask("Colony<Luna>")
      }
    }
    p1.playProject("ResearchColony", 20) {
      doTask("Colony<Luna>")
    }.expect("-20, Colony<Luna>")
  }

  @Test
  fun `but still not over the max of three`() {
    p1.godMode().manual("Colony<Luna>")
    p2.godMode().manual("Colony<Luna>")
    p2.godMode().manual("Colony<Luna>")
    assertThrows<LimitsException> {
      p1.playProject("ResearchColony", 20) {
        doTask("Colony<Luna>")
      }
    }
  }
}
