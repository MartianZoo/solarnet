package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class TitanShuttlesTest : TfmSandboxTest() {
  @Test
  internal fun `Can convert floaters without Colonies or Venus Next`() {
    newTestGame(addOptions = "-ColoniesExpansion, -VenusNextExpansion, TitanShuttles")
    kim.exMachina("$TitanShuttles, 7 Floater<$TitanShuttles>")

    kim.inTurn {
          doTask("UseCardAction<TitanShuttles, Action2>")
          doTasks("-5 Floater THEN 5 Titanium", "5 Titanium")
        }
        .expect("-5 Floater<$TitanShuttles>, 5 Titanium")
  }

  @Test
  internal fun `Cannot underpay its floater cost`() {
    newTestGame()
    kim.exMachina("$TitanShuttles, 7 Floater<$TitanShuttles>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("UseCardAction<TitanShuttles, Action2>")
        // Select the conversion to verify that its payment and reward share the same amount.
        selectTask("-X! Floater<TitanShuttles> THEN X! Titanium")
        doTask("-4 Floater THEN 5 Titanium")
      }
    }
  }
}
