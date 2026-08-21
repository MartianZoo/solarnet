package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class MangroveTest : CardTest() {
  // FAQ: "placed on any ocean area even if it is not adjacent"
  @Test
  fun `Can be placed on an ocean area despite a distant city`() {
    newGame()
    p1.manual("CityTile<Tharsis_9_5>")
    p1.manual("$Mangrove") { doTask("GreeneryTile<Tharsis_1_2>") }
        .expect("GreeneryTile<Tharsis_1_2>, OxygenStep, TerraformRating")
  }
}
