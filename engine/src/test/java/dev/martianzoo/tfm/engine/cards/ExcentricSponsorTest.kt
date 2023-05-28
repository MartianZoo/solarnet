package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.execapi.PlayerSession.Companion.session
import dev.martianzoo.tfm.execapi.TerraformingMars.phase
import dev.martianzoo.tfm.execapi.TerraformingMars.playCorp
import dev.martianzoo.tfm.execapi.TerraformingMars.turn
import org.junit.jupiter.api.Test

class ExcentricSponsorTest {
  @Test
  fun excentricSponsor() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))

    with(game.session(PLAYER1)) {
      playCorp("InterplanetaryCinematics", 7)
      phase("Prelude")

      // a little gory, since we can't use playCard()
      turn(
          "ExcentricSponsor",
          "PlayCard<Class<ProjectCard>, Class<NitrogenRichAsteroid>>",
          "6 Pay FROM M",
          "Ok", // the damn titanium
      )
      assertCounts(0 to "Owed", 5 to "M", 1 to "ExcentricSponsor", 1 to "PlayedEvent")
    }
  }
}
