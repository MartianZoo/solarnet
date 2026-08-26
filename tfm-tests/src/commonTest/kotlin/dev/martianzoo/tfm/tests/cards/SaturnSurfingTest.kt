package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SaturnSurfingTest : CardTest() {
  @Test
  internal fun `Converts one of six floaters into five mc`() {
    initializeSaturnSurfing()
    p1.cardAction1(SaturnSurfing).expect("-Floater, 5 MC")
  }

  @Test
  internal fun `Converts one of five floaters into five mc`() {
    initializeSaturnSurfing(floatersRemoved = 1)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 5 MC")
  }

  @Test
  internal fun `Converts one of four floaters into four mc`() {
    initializeSaturnSurfing(floatersRemoved = 2)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 4 MC")
  }

  @Test
  internal fun `Converts one of three floaters into three mc`() {
    initializeSaturnSurfing(floatersRemoved = 3)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 3 MC")
  }

  @Test
  internal fun `Converts one of two floaters into two mc`() {
    initializeSaturnSurfing(floatersRemoved = 4)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 2 MC")
  }

  @Test
  internal fun `Converts its last floater into one mc`() {
    initializeSaturnSurfing(floatersRemoved = 5)
    p1.cardAction1(SaturnSurfing).expect("-Floater, MC")
  }

  private fun initializeSaturnSurfing(floatersRemoved: Int = 0) {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Teractor, $EarthOffice, $AcquiredCompany, $MediaGroup, $Cartel, $SaturnSurfing")
    if (floatersRemoved > 0) p1.manual("-$floatersRemoved Floater<$SaturnSurfing>")
  }
}
