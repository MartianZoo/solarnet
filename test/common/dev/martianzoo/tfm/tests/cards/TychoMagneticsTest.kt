package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TychoMagneticsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Keeps a card from its selection`() {
    kim.exMachina("$TychoMagnetics")
    kim.setToExMachina(4, "Energy")

    kim.inTurn {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-4 Energy",
          "4 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }
}
