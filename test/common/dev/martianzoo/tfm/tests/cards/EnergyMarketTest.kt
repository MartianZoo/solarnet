package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.EnergyMarket
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class EnergyMarketTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Spends two MC per chosen Energy`() {
    kim.exMachina("$EnergyMarket")
    kim.setToExMachina(6, "MC")

    kim.inTurn {
          doTasks(*cardActionTasks("EnergyMarket"))
          selectOnlyTask(x = 3)
          doTask("3 Energy")
        }
        .expect("-6 MC")
  }

  @Test
  internal fun `Must afford twice the chosen amount and can retry a smaller amount`() {
    kim.exMachina("$EnergyMarket")
    kim.setToExMachina(5, "MC")

    shouldThrow<LimitsException> {
          kim.inTurn {
            doTasks(*cardActionTasks("EnergyMarket"))
            selectOnlyTask(x = 3)
            doTask("3 Energy")
          }
        }
        .message
        .shouldContain("cannot remove 6 `MC<Kim>`: maximum available is 5")
    kim.assertCounts(5 to "MC", 0 to "Energy", 0 to "ActionUsedMarker<EnergyMarket>")

    kim.inTurn {
          doTasks(*cardActionTasks("EnergyMarket"))
          selectOnlyTask(x = 2)
          doTask("2 Energy")
        }
        .expect("-4 MC")
  }
}
