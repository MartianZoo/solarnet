package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SteelworksTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "used even after the oxygen has been maxed out"
  @Test
  internal fun `Can be used after oxygen is maxed`() {
    kim.exMachina("$Steelworks")
    kim.setToExMachina(13, "OxygenStep")
    kim.setToExMachina(4, "Energy")
    kim.stdProject("GreeneryProject") { placeTile(5, 2) }

    kim.cardAction1(Steelworks).expect("-4 Energy, 2 Steel, 0 OxygenStep, 0 TerraformRating")
  }
}
