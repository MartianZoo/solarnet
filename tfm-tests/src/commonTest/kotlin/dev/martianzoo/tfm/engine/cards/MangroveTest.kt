package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class MangroveTest : CardTest() {
  // FAQ: "placed on any ocean area even if it is not adjacent"
  @Test
  internal fun `Can be placed on an ocean area despite a distant city`() {
    newGame()
    p1.manual("CityTile<Tharsis_9_5>")
    p1.manual("$Mangrove") { placeTile(1, 2) }
        .expect("GreeneryTile<Tharsis_1_2>, OxygenStep, TerraformRating")
  }
}
