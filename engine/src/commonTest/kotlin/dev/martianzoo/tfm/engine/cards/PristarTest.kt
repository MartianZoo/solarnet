package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class PristarTest : CardTest() {
  @Test
  fun `without a TR increase, runs Pristar production`() {
    newGame("TerraformingMars,TharsisMapOption,TurmoilCardPack")
    p1.manual("Pristar")
    engine.manual("ProductionPhase FROM Phase").expect("Preservation")
  }

  @Test
  fun `after a TR increase, runs Pristar production`() {
    newGame("TerraformingMars,TharsisMapOption,TurmoilCardPack")
    p1.manual("Pristar, TerraformRating")
    engine.manual("ProductionPhase FROM Phase").expect("0 Preservation")
  }
}
