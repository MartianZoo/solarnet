package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ResearchColonyTest : ColoniesCardTest() {
  @Test
  fun `with a colony on Luna, plays Research Colony`() {
    p1.manual("ProjectCard, 20, Colony<Luna>")
    p1.playProject(ResearchColony, 20) { doTask("Colony<Luna>") }.expect("-20, Colony<Luna>")
  }

  @Test
  fun `with a colony on Luna, tries to build another using a standard project`() {
    p1.manual("17, Colony<Luna>")
    shouldThrow<NarrowingException> {
      p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") }
    }
  }

  @Test
  fun `with three colonies on Luna, tries to play Research Colony`() {
    p1.manual("ProjectCard, 20, Colony<Luna>, 2 Colony<Player2, Luna>")
    shouldThrow<LimitsException> {
      p1.playProject(ResearchColony, 20) { doTask("Colony<Luna>") }
    }
  }
}
