package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SponsoredAcademiesTest {
  @Test
  fun sponsoredAcademies() {
    val game = Engine.newGame(GameSetup(Canon, "BRMV", 2))

    game.tfm(PLAYER1).playCorp("Phobolog", 5)

    with(game.tfm(PLAYER2)) {
      playCorp("Ecoline", 1)
      phase("Action")
      assertThrows<LimitsException>("nothing to discard") { playProject("SponsoredAcademies", 9) }

      godMode().sneak("ProjectCard")

      assertCounts(2 to "ProjectCard")
      assertCounts(5 to "ProjectCard<P1>")

      playProject("SponsoredAcademies", 9)
      assertCounts(3 to "ProjectCard") // played 1, discarded 1, drew 3
      assertCounts(6 to "ProjectCard<P1>")
    }
  }
}
