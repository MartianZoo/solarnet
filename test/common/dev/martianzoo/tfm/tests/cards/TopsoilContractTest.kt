package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TopsoilContractTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Its microbe tag triggers Decomposers and pays for that microbe`() {
    kim.exMachina("$Decomposers")

    kim.playProject(TopsoilContract, 8).expect("Microbe<$Decomposers>, -7 MC")
  }
}
