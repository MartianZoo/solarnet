package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class SteelworksTest : CardTest() {
  // FAQ: "used even after the oxygen has been maxed out"
  @Test
  fun `with oxygen maxed, uses Steelworks`() {
    newGame()
    p1.manual("Steelworks, 4 Energy")
    engine.manual("14 OxygenStep")
    engine.phase("Action")
    p1.cardAction1("Steelworks").expect("-4 Energy, 2 Steel")
    p1.assertCounts(14 to "OxygenStep", 20 to "TerraformRating")
  }
}
