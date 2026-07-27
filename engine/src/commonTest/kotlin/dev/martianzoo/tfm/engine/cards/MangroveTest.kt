package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class MangroveTest : CardTest() {
  // FAQ: "placed on any ocean area even if it is not adjacent"
  @Test
  fun `with a distant city, places Mangrove on an ocean area`() {
    newGame()
    p1.manual("CityTile<Tharsis_9_5>")
    p1.manual("Mangrove") { doTask("GreeneryTile<Tharsis_1_2>") }
        .expect("GreeneryTile<Tharsis_1_2>, OxygenStep, TerraformRating")
  }
}
