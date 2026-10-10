package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class L1TradeTerminalTest : TfmSandboxTest() {
  @Test
  internal fun `L1 Trade Terminal chooses three distinct cards when more are eligible`() {
    newTestGame("L1TradeTerminal, FloatingTradeHub, FloatingRefinery, CloudTourism")
    kim.exMachina(
        "$FloatingHabs, Floater<$FloatingHabs>, " +
            "$AerialMappers, Floater<$AerialMappers>, " +
            "$FloatingRefinery, Floater<$FloatingRefinery>, " +
            "$CloudTourism, Floater<$CloudTourism>, $FloatingTradeHub"
    )
    stan.exMachina("$JetStreamMicroscrappers, Floater<$JetStreamMicroscrappers>")

    shouldThrow<DeadEndException> {
      kim.playProject(L1TradeTerminal, 25) {
        addCardResources(AerialMappers)
        addCardResources(FloatingRefinery)
        declineTask("Floater<$CloudTourism>?")
        // Decline the remaining Floating Habs.
        doTask("Ok")
      }
    }

    shouldThrow<GameplayException> {
      kim.playProject(L1TradeTerminal, 25) {
        addCardResources(AerialMappers)
        addCardResources(FloatingRefinery)
        addCardResources(CloudTourism)
        doTask("Floater<$FloatingHabs>")
      }
    }
    kim.count("$L1TradeTerminal") shouldBe 0

    kim.playProject(L1TradeTerminal, 25) {
          shouldThrow<TaskException> {
            doTask("Floater<Stan, $JetStreamMicroscrappers<Stan>>")
          }
          addCardResources(AerialMappers)
          addCardResources(FloatingRefinery)
          addCardResources(CloudTourism)
          // Decline the remaining Floating Habs.
          declineTask()
        }
        .expect(
            "Floater<$AerialMappers>, Floater<$FloatingRefinery>, Floater<$CloudTourism>, " +
                "0 Floater<$FloatingHabs>, 0 Floater<$FloatingTradeHub>, " +
                "0 Floater<Stan, $JetStreamMicroscrappers<Stan>>"
        )
  }

  @Test
  internal fun `L1 Trade Terminal must give to its sole eligible card`() {
    newTestGame("L1TradeTerminal")
    kim.exMachina("$FloatingHabs, Floater<$FloatingHabs>")

    shouldThrow<DeadEndException> {
      kim.playProject(L1TradeTerminal, 25) { doTask("Ok") }
    }

    kim.playProject(L1TradeTerminal, 25) {
          addCardResources(FloatingHabs)
        }
        .expect("Floater<$FloatingHabs>")
  }

  @Test
  internal fun `L1 Trade Terminal must give to all three eligible cards`() {
    newTestGame("L1TradeTerminal")
    kim.exMachina(
        "$FloatingHabs, Floater<$FloatingHabs>, " +
            "$AerialMappers, Floater<$AerialMappers>, " +
            "$VenusianInsects, Microbe<$VenusianInsects>"
    )

    shouldThrow<DeadEndException> {
      kim.playProject(L1TradeTerminal, 25) {
        addCardResources(FloatingHabs)
        addCardResources(VenusianInsects)
        doTask("Ok")
      }
    }

    kim.playProject(L1TradeTerminal, 25) {
          addCardResources(FloatingHabs)
          addCardResources(VenusianInsects)
          addCardResources(AerialMappers)
        }
        .expect("Floater<$FloatingHabs>, Microbe<$VenusianInsects>, Floater<$AerialMappers>")
  }

  @Test
  internal fun `L1 Trade Terminal cannot give twice to one card or skip one of two`() {
    newTestGame("L1TradeTerminal, FloatingTradeHub")
    kim.exMachina(
        "$FloatingHabs, Floater<$FloatingHabs>, " +
            "$VenusianInsects, Microbe<$VenusianInsects>, " +
            "$FloatingTradeHub"
    )

    shouldThrow<NarrowingException> {
      kim.playProject(L1TradeTerminal, 25) {
        addCardResources(FloatingHabs)
        doTask("Floater<$FloatingHabs>")
      }
    }
    kim.count("$L1TradeTerminal") shouldBe 0

    shouldThrow<DeadEndException> {
      kim.playProject(L1TradeTerminal, 25) {
        addCardResources(FloatingHabs)
        doTask("Ok")
      }
    }

    kim.playProject(L1TradeTerminal, 25) {
          addCardResources(FloatingHabs)
          addCardResources(VenusianInsects)
        }
        .expect("Floater<$FloatingHabs>, Microbe<$VenusianInsects>, 0 Floater<$FloatingTradeHub>")
  }

  @Test
  internal fun `L1 Trade Terminal can be played without an eligible card`() {
    newTestGame("L1TradeTerminal, FloatingTradeHub")
    stan.exMachina("$FloatingTradeHub, Floater<$FloatingTradeHub>")

    kim.playProject(L1TradeTerminal, 25)
        .expect("$L1TradeTerminal, 0 Floater<Stan, $FloatingTradeHub<Stan>>")
  }
}
