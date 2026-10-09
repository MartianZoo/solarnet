package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SolarLogisticsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Draws a card for a space event played by its owner`() {
    kim.exMachina("$SolarLogistics")
    // The drawn card replaces the played event in hand.
    kim.playProject(ImportedGhg, 5).expect("0 ProjectCard")
  }

  @Test
  internal fun `Draws a card for a space event played by an opponent`() {
    kim.exMachina("$SolarLogistics")
    stan.playProject(TechnologyDemonstration, 5).expect("ProjectCard<Kim>")
  }
}
