package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchColony
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ResearchColonyTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Can be played when its player already has a colony on Luna`() {
    kim.exMachina("Colony<Luna>")

    kim.playProject(ResearchColony, 20) { doTask("Colony<Luna>") }.expect("-20 MC, Colony<Luna>")
  }

  @Test
  internal fun `Cannot be played on a colony tile that already has three colonies`() {
    kim.exMachina("Colony<Kim, Luna>, Colony<Stan, Luna>, Colony<Rob, Luna>")

    shouldThrow<LimitsException> {
      kim.playProject(ResearchColony, 20) { doTask("Colony<Luna>") }
    }
  }
}
