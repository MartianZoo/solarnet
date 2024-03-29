package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class ValleyTrustTest {
  @Test
  fun valleyTrust() {
    val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("ValleyTrust", 5)
      assertCounts(5 to "ProjectCard", 22 to "M")

      phase("Action")
      assertCounts(1 to "Mandate")
      assertCounts(0 to "PreludeCard")

      stdAction("HandleMandates") {
        assertCounts(1 to "PreludeCard")

        playPrelude("MartianIndustries")
        assertCounts(1 to "PROD[S]", 1 to "PROD[E]")
        assertCounts(0 to "PreludeCard")
      }
    }
  }
}
