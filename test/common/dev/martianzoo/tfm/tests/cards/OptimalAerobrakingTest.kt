package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class OptimalAerobrakingTest : ProjectCardTest() {

  @Test
  internal fun `Pays when its owner plays an asteroid event`() {
    kim.exMachina("$OptimalAerobraking")

    kim.playProject(AsteroidCard, 14).expect("-11 MC, 3 Heat")
  }
}
