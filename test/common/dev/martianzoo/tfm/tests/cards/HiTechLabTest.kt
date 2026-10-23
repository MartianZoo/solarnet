package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.HiTechLab
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HiTechLabTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot finish without choosing an amount`() {
    kim.exMachina("$HiTechLab, 3 Energy")

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        doTask("UseCardAction<HiTechLab, Action1>")
      }
    }
  }

  @Test
  internal fun `Keeps a card from its selection`() {
    kim.exMachina("$HiTechLab")
    kim.setToExMachina(3, "Energy")

    kim.inTurn {
          doTask("UseCardAction<HiTechLab, Action1>")
          selectOnlyTask(x = 3)
          doTask("3 ProjectCard<Kim, Selecting>")
          doTask("ProjectCard<Hand FROM Selecting>")
        }
        .expect("-3 Energy")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }
}
