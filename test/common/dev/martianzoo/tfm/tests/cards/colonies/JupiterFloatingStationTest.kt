package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.AtmoCollectors
import dev.martianzoo.tfm.tests.cards.cardnames.JupiterFloatingStation
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class JupiterFloatingStationTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `First action adds a floater to another Jovian card`() {
    kim.exMachina("$JupiterFloatingStation, $TitanShuttles")

    kim.cardAction1(JupiterFloatingStation) { addCardResources(TitanShuttles) }
        .expect("Floater<$TitanShuttles>")
  }

  @Test
  internal fun `First action rejects a card without a Jovian tag`() {
    kim.exMachina("$JupiterFloatingStation, $AtmoCollectors, Floater<$AtmoCollectors>")

    shouldThrow<NarrowingException> {
      kim.cardAction1(JupiterFloatingStation) { doTask("Floater<$AtmoCollectors>") }
    }
  }

  @Test
  internal fun `Second action pays at most four MC`() {
    kim.exMachina("$JupiterFloatingStation, 6 Floater<$JupiterFloatingStation>")

    kim.cardAction2(JupiterFloatingStation).expect("4 MC")
  }
}
