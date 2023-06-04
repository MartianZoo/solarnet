package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TerraformingMarsApi.Companion.tfm
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class ExcentricSponsorTest {
  @Test
  fun excentricSponsor() {
    val game = Engine.newGame(GameSetup(Canon, "BRHP", 2))

    with(game.tfm(PLAYER1)) {
      playCorp("Ecoline", 4)
      phase("Prelude")

      turns.turn {
        doTask("ExcentricSponsor")
        playProject("NitrogenRichAsteroid", 6)
      }

      assertCounts(0 to "Owed", 18 to "M", 1 to "ExcentricSponsor", 1 to "PlayedEvent")
    }
  }
}
