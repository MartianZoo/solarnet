package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
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
          doTasks("UseCardAction<JupiterFloatingStation, Action1>", "Floater<TitanShuttles>*")
        }
        .expect("Floater<$TitanShuttles>")
  }

  @Test
  internal fun `First action rejects a card without a Jovian tag`() {
    kim.exMachina("$JupiterFloatingStation, $AtmoCollectors, Floater<$AtmoCollectors>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectTask("StandardAction OR Pass")
        doTask("UseCardAction<JupiterFloatingStation, Action1>")
        selectTask("Floater<CardFront(HAS JovianTag)>*")
        doTask("Floater<AtmoCollectors>")
      }
    }
  }

  @Test
  internal fun `Second action pays at most four MC`() {
    kim.exMachina("$JupiterFloatingStation, 6 Floater<$JupiterFloatingStation>")

    kim.inTurn {
          doTask("UseCardAction<JupiterFloatingStation, Action2>")
          selectTask("MC! / Floater<JupiterFloatingStation> MAX 4")
        }
        .expect("4 MC")
  }
}
