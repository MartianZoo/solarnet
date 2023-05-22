package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class MergerTest {

  @Test
  fun valleyTrustAndCelestic() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPVX", 2))
    with(game.session(PLAYER1)) {
      playCorp("ValleyTrust", 5)
      assertCounts(5 to "ProjectCard", 22 to "M")

      phase("Prelude")
      turn("UnmiContractor")
      turn("Merger") { matchTask("PlayCard<Class<CorporationCard>, Class<Celestic>>") }

      phase("Action")
      assertCounts(2 to "Mandate")
      assertCounts(0 to "PreludeCard")
      assertCounts(6 to "ProjectCard")

      turn("UseAllMandates") {
        assertCounts(8 to "ProjectCard")
        assertCounts(1 to "PreludeCard")
        task("PlayCard<Class<PreludeCard>, Class<SocietySupport>>")
      }
    }
  }
}
