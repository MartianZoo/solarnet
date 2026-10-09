package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AdvertisingTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Does not trigger for a 19-cost card`() {
    kim.exMachina("$Advertising")

    kim.playProject(LunarExports, 19) { doTask("PROD[5 MC]") }.expect("PROD[5 MC]")
  }

  @Test
  internal fun `Triggers for a 20-cost card`() {
    kim.exMachina("$Advertising")

    kim.playProject(GanymedeColony, 20).expect("PROD[1 MC]")
  }
}
