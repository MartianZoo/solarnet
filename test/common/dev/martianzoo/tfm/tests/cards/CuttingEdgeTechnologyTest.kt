package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CuttingEdgeTechnologyTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Discounts a card with a requirement`() {
    kim.exMachina("$CuttingEdgeTechnology")

    kim.playProject(DustSeals, 0).expect("0 MC")
  }

  @Test
  internal fun `Does not discount a card without a requirement`() {
    kim.exMachina("$CuttingEdgeTechnology")

    kim.playProject(Advertising, 4).expect("-4 MC")
  }
}
