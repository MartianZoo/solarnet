package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class MaxwellBaseTest : CardTest() {
  @Test
  fun `builds off Mars and can supply another Venus card`() {
    val game = newGame(Canon.fromOptionCodes("BMV", 2))
    with(game.tfm(PLAYER1)) {
      phase("Action")
      manual("PROD[Energy], ForcedPrecipitation")
      manual("MaxwellBase").expect("CityTile<Area238>, PROD[-Energy]")

      cardAction1("MaxwellBase") { doTask("Floater<ForcedPrecipitation>") }
    }
  }
}
