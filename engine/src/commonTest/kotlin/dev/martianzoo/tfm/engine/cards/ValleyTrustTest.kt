package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class ValleyTrustTest : CardTest() {
  @Test
  fun valleyTrust() {
    val game = newGame("BRMP", 2)
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("ValleyTrust", 5).expect("5 ProjectCard, 22")

      phase("Action")
      assertCounts(1 to "Mandate")
      assertCounts(0 to "PreludeCard")

      stdAction("HandleMandates") {
            assertCounts(1 to "PreludeCard")

            playPrelude("MartianIndustries")
            assertCounts(0 to "PreludeCard")
          }
          .expect("PROD[Steel, Energy]")
    }
  }
}
