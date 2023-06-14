package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class ExcentricSponsorTest {
  @Test
  fun excentricSponsor() {
    val game = Engine.newGame(GameSetup(Canon, "BRHP", 2))

    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("Ecoline", 4)
      phase("Prelude")

      playPrelude("ExcentricSponsor") { playProject("NitrogenRichAsteroid", 6) }

      assertCounts(0 to "Owed", 18 to "M", 1 to "ExcentricSponsor", 1 to "PlayedEvent")
    }
  }
}
