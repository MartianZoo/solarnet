package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class SaturnSurfingTest : CardTest() {
  @Test
  fun `Converts one of six floaters into five megacredits`() {
    initializeSaturnSurfing()
    p1.cardAction1(SaturnSurfing).expect("-Floater, 5 Megacredit")
  }

  @Test
  fun `Converts one of five floaters into five megacredits`() {
    initializeSaturnSurfing(floatersRemoved = 1)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 5 Megacredit")
  }

  @Test
  fun `Converts one of four floaters into four megacredits`() {
    initializeSaturnSurfing(floatersRemoved = 2)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 4 Megacredit")
  }

  @Test
  fun `Converts one of three floaters into three megacredits`() {
    initializeSaturnSurfing(floatersRemoved = 3)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 3 Megacredit")
  }

  @Test
  fun `Converts one of two floaters into two megacredits`() {
    initializeSaturnSurfing(floatersRemoved = 4)
    p1.cardAction1(SaturnSurfing).expect("-Floater, 2 Megacredit")
  }

  @Test
  fun `Converts its last floater into one megacredit`() {
    initializeSaturnSurfing(floatersRemoved = 5)
    p1.cardAction1(SaturnSurfing).expect("-Floater, Megacredit")
  }

  private fun initializeSaturnSurfing(floatersRemoved: Int = 0) {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Teractor, $EarthOffice, $AcquiredCompany, $MediaGroup, $Cartel, $SaturnSurfing")
    if (floatersRemoved > 0) p1.manual("-$floatersRemoved Floater<$SaturnSurfing>")
  }
}
