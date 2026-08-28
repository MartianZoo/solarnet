package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ResearchColonyTest : ColoniesCardTest() {
  @Test
  internal fun `Can be played when its player already has a colony on Luna`() {
    p1.manual("ProjectCard, 20 MC, Colony<Luna>")
    p1.playProject(ResearchColony, 20) { doTask("Colony<Luna>") }.expect("-20 MC, Colony<Luna>")
  }

  @Test
  internal fun `Cannot build a second colony on the same colony tile`() {
    p1.manual("17 MC, Colony<Luna>")
    shouldThrow<NarrowingException> {
      p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") }
    }
  }

  @Test
  internal fun `Cannot be played on a colony tile that already has three colonies`() {
    p1.manual("ProjectCard, 20 MC, Colony<Luna>, 2 Colony<Player2, Luna>")
    shouldThrow<LimitsException> {
      p1.playProject(ResearchColony, 20) { doTask("Colony<Luna>") }
    }
  }
}
