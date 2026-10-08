package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MartianLumberCorporationTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Plants can pay for a building card`() {
    kim.exMachina("$MartianLumberCorp")
    kim.setToExMachina(2, "Plant")

    kim.playProject(Mine, 1) { doTask("-Plant") }.expect("-Plant")
  }
}
