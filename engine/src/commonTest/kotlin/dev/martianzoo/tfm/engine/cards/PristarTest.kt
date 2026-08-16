package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class PristarTest : CardTest() {
  @Test
  fun `without a TR increase, runs Pristar production`() {
    newGame(TurmoilCardPack)
    p1.manual("Pristar")
    engine.phase("Production")
    p1.assertCounts(1 to "Preservation")
  }

  @Test
  fun `after a TR increase, runs Pristar production`() {
    newGame(TurmoilCardPack)
    p1.manual("Pristar, TerraformRating")
    engine.phase("Production")
    p1.assertCounts(0 to "Preservation")
  }
}
