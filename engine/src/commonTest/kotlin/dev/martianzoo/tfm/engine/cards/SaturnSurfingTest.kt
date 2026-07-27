package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class SaturnSurfingTest : CardTest() {
  @Test
  fun `with six floaters, uses Saturn Surfing`() {
    initializeSaturnSurfing()
    p1.cardAction1("SaturnSurfing").expect("-Floater, 5 Megacredit")
  }

  @Test
  fun `with five floaters, uses Saturn Surfing`() {
    initializeSaturnSurfing(floatersRemoved = 1)
    p1.cardAction1("SaturnSurfing").expect("-Floater, 5 Megacredit")
  }

  @Test
  fun `with four floaters, uses Saturn Surfing`() {
    initializeSaturnSurfing(floatersRemoved = 2)
    p1.cardAction1("SaturnSurfing").expect("-Floater, 4 Megacredit")
  }

  @Test
  fun `with three floaters, uses Saturn Surfing`() {
    initializeSaturnSurfing(floatersRemoved = 3)
    p1.cardAction1("SaturnSurfing").expect("-Floater, 3 Megacredit")
  }

  @Test
  fun `with two floaters, uses Saturn Surfing`() {
    initializeSaturnSurfing(floatersRemoved = 4)
    p1.cardAction1("SaturnSurfing").expect("-Floater, 2 Megacredit")
  }

  @Test
  fun `with one floater, uses Saturn Surfing`() {
    initializeSaturnSurfing(floatersRemoved = 5)
    p1.cardAction1("SaturnSurfing").expect("-Floater, Megacredit")
  }

  private fun initializeSaturnSurfing(floatersRemoved: Int = 0) {
    newGame("BMRX")
    engine.phase("Action")
    p1.manual("Teractor, EarthOffice, AcquiredCompany, MediaGroup, Cartel, SaturnSurfing")
    if (floatersRemoved > 0) p1.manual("-$floatersRemoved Floater<SaturnSurfing>")
  }
}
