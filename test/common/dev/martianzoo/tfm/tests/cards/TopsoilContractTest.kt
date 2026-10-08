package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TopsoilContractTest : ProjectCardTest() {
  @Test
  internal fun `Its own tag triggers Decomposers and pays for the resulting microbe`() {
    kim.exMachina("$Decomposers")

    kim.playProject(TopsoilContract, 8).expect("Microbe<$Decomposers>, 3 Plant, -7 MC")
  }
}
