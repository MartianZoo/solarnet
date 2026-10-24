package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.AtmoCollectors
import dev.martianzoo.tfm.tests.cards.cardnames.JupiterFloatingStation
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class JupiterFloatingStationTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `First action adds a floater to another Jovian card`() {
    kim.exMachina("$JupiterFloatingStation, $TitanShuttles")

    kim.inTurn {
          doTasks(*cardActionTasks("JupiterFloatingStation"), "Floater<TitanShuttles>*")
        }
        .expect("Floater<$TitanShuttles>")
  }

  @Test
  internal fun `First action rejects a card without a Jovian tag`() {
    kim.exMachina("$JupiterFloatingStation, $AtmoCollectors, Floater<$AtmoCollectors>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(*cardActionTasks("JupiterFloatingStation"))
        // Select the Jovian-only offer before attempting to use a non-Jovian card.
        selectTask("Floater<CardFront(HAS JovianTag)>*")
        doTask("Floater<AtmoCollectors>")
      }
    }
  }

  @Test
  internal fun `Second action pays at most four MC`() {
    kim.exMachina("$JupiterFloatingStation, 6 Floater<$JupiterFloatingStation>")

    kim.inTurn {
      doTasks(*cardActionTasks("JupiterFloatingStation", "Action2"))
      doTask("4 MC")
    }
  }
}
