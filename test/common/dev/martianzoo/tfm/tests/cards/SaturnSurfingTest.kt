package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SaturnSurfingTest : ProjectCardTest() {
  @Test
  internal fun `Caps its payout at five MC with six floaters`() {
    kim.exMachina("$SaturnSurfing")
    kim.setToExMachina(6, "Floater<$SaturnSurfing>")

    kim.cardAction1(SaturnSurfing).expect("-Floater, 5 MC")
  }

  @Test
  internal fun `Pays four MC with four floaters`() {
    kim.exMachina("$SaturnSurfing")
    kim.setToExMachina(4, "Floater<$SaturnSurfing>")

    kim.cardAction1(SaturnSurfing).expect("-Floater, 4 MC")
  }
}
