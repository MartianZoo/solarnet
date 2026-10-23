package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.TychoMagnetics
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TychoMagneticsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Keeps a card from its selection`() {
    kim.exMachina("$TychoMagnetics")
    kim.setToExMachina(4, "Energy")

    kim.inTurn {
          doTask("UseCardAction<TychoMagnetics, Action1>")
          selectOnlyTask(x = 4)
          doTask("4 ProjectCard<Kim, Selecting>")
          doTask("ProjectCard<Hand FROM Selecting>")
        }
        .expect("-4 Energy")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }
}
