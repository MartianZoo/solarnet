package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class TollStationTest : CardTest() {
  @Test
  fun `counts opponents' space tags but not the owner's`() {
    val game = newGame(GameSetup(Canon, "BMR", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    // Can't just sneak tags directly - they have a dependency!
    p1.godMode().manual("AsteroidMining, TransNeptuneProbe").expect("2 SpaceTag")
    p2.godMode().manual("VestaShipyard, SpaceElevator, SolarWindPower").expect("3 SpaceTag")

    p1.godMode().manual("TollStation").expect("PROD[3 Megacredit]")
  }
}
