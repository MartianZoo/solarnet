package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class MangroveTest : CardTest() {
  // FAQ: "placed on any ocean area even if it is not adjacent"
  @Test
  fun `Mangrove can place a nonadjacent greenery on an ocean area`() {
    val p1 = newGame("BM", 2).tfm(PLAYER1)
    p1.manual("CityTile<Tharsis_9_5>")

    p1.manual("Mangrove") { doTask("GreeneryTile<Tharsis_1_2>") }
        .expect("GreeneryTile<Tharsis_1_2>, OxygenStep, TerraformRating")
  }
}
