package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
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
      kim.inTurn {
        // Decline the remaining Floating Habs.
        doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
        doTasks("L1TradeTerminal FROM ProjectCard")
        // Expand the bonus into a separate resource choice for each eligible card.
        selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        doTasks("Floater<AerialMappers>", "Floater<FloatingRefinery>")
        doTask("0 Floater<CloudTourism>")
        doTask("0 Floater<FloatingHabs>")
      }
    }

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
        doTasks("L1TradeTerminal FROM ProjectCard")
        // Expand the bonus into a separate resource choice for each eligible card.
        selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        doTasks("Floater<AerialMappers>", "Floater<FloatingRefinery>", "Floater<CloudTourism>")
        doTask("Floater<FloatingHabs>")
      }
    }
    kim.count("$L1TradeTerminal") shouldBe 0

    kim.inTurn {
          doTasks(
              "PlayProject<Class<L1TradeTerminal>>",
              "-25 MC",
              "L1TradeTerminal FROM ProjectCard",
          )
          // Expand the bonus into a separate resource choice for each eligible card.
          selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
          shouldThrow<TaskException> {
            doTask("Floater<Stan, $JetStreamMicroscrappers<Stan>>")
          }
          doTasks("Floater<AerialMappers>", "Floater<FloatingRefinery>", "Floater<CloudTourism>")
          // Decline the remaining Floating Habs.
          doTask("0 Floater<FloatingHabs>")
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
      kim.inTurn {
        doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
        doTasks("L1TradeTerminal FROM ProjectCard")
        // Expand the bonus into a separate resource choice for each eligible card.
        selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        doTask("0 Floater<FloatingHabs>")
      }
    }

    kim.inTurn {
      doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
      doTasks("L1TradeTerminal FROM ProjectCard")
      // Expand the bonus into a separate resource choice for each eligible card.
      selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
      doTask("Floater<FloatingHabs>")
    }
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
      kim.inTurn {
        doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
        doTasks("L1TradeTerminal FROM ProjectCard")
        // Expand the bonus into a separate resource choice for each eligible card.
        selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        doTasks("Floater<FloatingHabs>", "Microbe<VenusianInsects>")
        doTask("0 Floater<AerialMappers>")
      }
    }

    kim.inTurn {
      doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
      doTasks("L1TradeTerminal FROM ProjectCard")
      // Expand the bonus into a separate resource choice for each eligible card.
      selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
      doTasks("Floater<FloatingHabs>", "Microbe<VenusianInsects>")
      doTask("Floater<AerialMappers>")
    }
  }

  @Test
  internal fun `L1 Trade Terminal cannot give twice to one card or skip one of two`() {
    newTestGame("L1TradeTerminal, FloatingTradeHub")
    kim.exMachina(
        "$FloatingHabs, Floater<$FloatingHabs>, " +
            "$VenusianInsects, Microbe<$VenusianInsects>, " +
            "$FloatingTradeHub"
    )

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
        doTasks("L1TradeTerminal FROM ProjectCard")
        // Expand the bonus into a separate resource choice for each eligible card.
        selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        doTask("Floater<FloatingHabs>")
        doTask("Floater<FloatingHabs>")
      }
    }
    kim.count("$L1TradeTerminal") shouldBe 0

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
        doTasks("L1TradeTerminal FROM ProjectCard")
        // Expand the bonus into a separate resource choice for each eligible card.
        selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        doTask("Floater<FloatingHabs>")
        doTask("0 Microbe<VenusianInsects>")
      }
    }

    kim.inTurn {
          doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
          doTasks("L1TradeTerminal FROM ProjectCard")
          // Expand the bonus into a separate resource choice for each eligible card.
          selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
          doTask("Floater<FloatingHabs>")
          doTask("Microbe<VenusianInsects>")
        }
        .expect("0 Floater<$FloatingTradeHub>")
  }

  @Test
  internal fun `L1 Trade Terminal can be played without an eligible card`() {
    newTestGame("L1TradeTerminal, FloatingTradeHub")
    stan.exMachina("$FloatingTradeHub, Floater<$FloatingTradeHub>")

    kim.inTurn {
          doTasks("PlayProject<Class<L1TradeTerminal>>", "-25 MC")
          doTasks("L1TradeTerminal FROM ProjectCard")
          // Expand the bonus into a separate resource choice for each eligible card.
          selectTask("EACH @ResourceCard(HAS CardResource) { CardResource<@ResourceCard>? }")
        }
        .expect("0 Floater<Stan, $FloatingTradeHub<Stan>>")
  }
}
