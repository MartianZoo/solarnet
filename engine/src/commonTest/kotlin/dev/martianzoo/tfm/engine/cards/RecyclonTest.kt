package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class RecyclonTest : CardTest() {
  @Test
  fun `each building can add a microbe or spend two for plant production`() {
    val game = newGame(GameSetup(Canon, "BMRX", 2))
    val p1 = game.tfm(PLAYER1)
    with(p1) {
      godMode().manual("Recyclon").expect("Microbe<Recyclon>")

      godMode().manual("Mine").expect("Microbe<Recyclon>")

      godMode().manual("PowerPlantCard") { doTask("Microbe<Recyclon>!") }

      godMode().manual("TitaniumMine") {
        doTask("-2 Microbe<Recyclon> THEN PROD[Plant]")
      }
    }
  }
}
