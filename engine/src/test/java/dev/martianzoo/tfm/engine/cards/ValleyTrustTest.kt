package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.OldTfmHelpers.phase
import dev.martianzoo.tfm.engine.OldTfmHelpers.playCorp
import dev.martianzoo.tfm.engine.OldTfmHelpers.turn
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class ValleyTrustTest {
  @Test
  fun valleyTrust() {
    val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))
    with(game.session(PLAYER1)) {
      playCorp("ValleyTrust", 5)
      assertCounts(5 to "ProjectCard", 22 to "M")

      phase("Action")
      assertCounts(1 to "Mandate")
      assertCounts(0 to "PreludeCard")
      turn("UseAllMandates") {
        assertCounts(1 to "PreludeCard")
        task("PlayCard<Class<PreludeCard>, Class<MartianIndustries>>")
        task("Ok") // TODO damm stupid steel
        assertCounts(1 to "PROD[S]", 1 to "PROD[E]")
        assertCounts(0 to "PreludeCard")
      }
    }
  }
}
