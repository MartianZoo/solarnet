package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class RecessionTest : TfmSandboxTest() {
  @Test
  internal fun `Takes only available money while applying each opponents production loss`() {
    newTestGame("PreludeExpansion, Recession")
    stan.setToExMachina(4, "MC")
    stan.setToExMachina(-4, "PROD[MC]")
    rob.setToExMachina(5, "MC")
    rob.setToExMachina(2, "PROD[MC]")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<Recession>>",
              "Recession FROM PreludeCard",
              "10 MC",
          )
          // Expand the effect into separate losses for each other player.
          selectOnlyTask()
          doTask("-5* MC<Stan>")
          doTask("PROD[-MC<Stan>]")
          doTask("-5* MC<Rob>")
          doTask("PROD[-MC<Rob>]")
        }
        .expect("-4 MC<Stan>, -5 MC<Rob>, PROD[0 MC, -MC<Stan>, -MC<Rob>]")
  }

  @Test
  internal fun `An opponent at minimum production prevents play through Board of Directors`() {
    newTestGame("PreludeExpansion, Recession, BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()
    stan.setToExMachina(-5, "PROD[MC]")
    stan.setToExMachina(8, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseCardAction<BoardOfDirectors, Action1>")
        doTask("PreludeCard<Kim, Hand>")
        doTask("-12 MC")
        doTask("-Director<BoardOfDirectors>")
        doTasks("PlayCard<Class<PreludeCard>, Class<Recession>>", "Recession FROM PreludeCard")
        doTask("10 MC")
        // Expand the effect into separate losses for each other player.
        selectOnlyTask()
        doTask("-5* MC<Stan>")
        doTask("-5* MC<Rob>")
        doTask("PROD[-MC<Rob>]")
        doTask("PROD[-MC<Stan>]")
      }
    }
    kim.assertCounts(42 to "MC", 4 to "Director<$BoardOfDirectors>", 0 to "$Recession")
    stan.assertCounts(8 to "MC")
    stan.assertProds(-5 to "MC")
  }

  @Test
  internal fun `A fizzle refunds the owner without taking money or production from opponents`() {
    newTestGame("PreludeExpansion, Recession")
    stan.setToExMachina(-5, "PROD[MC]")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("PlayCard<Class<PreludeCard>, Class<Recession>>", "Recession FROM PreludeCard")
        doTask("10 MC")
        // Expand the effect into separate losses for each other player.
        selectOnlyTask()
        doTask("-5* MC<Stan>")
        doTask("-5* MC<Rob>")
        doTask("PROD[-MC<Rob>]")
        doTask("PROD[-MC<Stan>]")
      }
    }
    kim.inTurn { doTasks("-PreludeCard", "Audit", "15 MC") }
        .expect("0 MC<Stan>, 0 MC<Rob>, PROD[0 MC<Stan>, 0 MC<Rob>]")
  }
}
