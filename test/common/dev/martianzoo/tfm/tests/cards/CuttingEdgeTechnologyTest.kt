package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CuttingEdgeTechnologyTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Discounts a card with a requirement`() {
    kim.exMachina("$CuttingEdgeTechnology")

    kim.inTurn { doTasks(*playCardTasks("$DustSeals"), "$DustSeals FROM ProjectCard") }
        .expect("0 MC")
  }

  @Test
  internal fun `Does not discount a card without a requirement`() {
    kim.exMachina("$CuttingEdgeTechnology")

    kim.inTurn {
      doTasks(
          *playCardTasks("Advertising"),
          "4 Pay<> FROM MC",
          "Advertising FROM ProjectCard",
      )
    }
  }
}
