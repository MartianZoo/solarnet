package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.PowerInfrastructure
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PowerInfrastructureTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot finish without choosing an amount`() {
    kim.exMachina("$PowerInfrastructure, 5 Energy")

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        doTask("UseCardAction<PowerInfrastructure, Action1>")
      }
    }
  }

  @Test
  internal fun `Spends only the chosen energy and gains the same money`() {
    kim.exMachina("$PowerInfrastructure, 5 Energy")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTask("UseCardAction<PowerInfrastructure, Action1>")
          selectOnlyTask(x = 3)
          doTask("3 MC")
        }
        .expect("-3 Energy")
  }

  @Test
  internal fun `Cannot gain money for energy it does not have`() {
    kim.exMachina("$PowerInfrastructure, 2 Energy")
    kim.setToExMachina(0, "MC")

    shouldThrow<LimitsException> {
          kim.inTurn {
            doTask("UseCardAction<PowerInfrastructure, Action1>")
            selectOnlyTask(x = 3)
            doTask("3 MC")
          }
        }
        .message
        .shouldContain("cannot remove 3 `Energy<Kim>`: maximum available is 2")
    kim.assertCounts(2 to "Energy", 0 to "MC", 0 to "ActionUsedMarker<PowerInfrastructure>")
  }
}
