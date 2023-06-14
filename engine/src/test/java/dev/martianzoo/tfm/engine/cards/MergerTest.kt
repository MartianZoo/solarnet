package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class MergerTest {

  @Test
  fun valleyTrustAndCelestic() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPVX", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("ValleyTrust", 5)
      assertCounts(5 to "ProjectCard", 22 to "M")

      phase("Prelude")
      playPrelude("UnmiContractor")
      playPrelude("Merger") { doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>") }

      phase("Action")
      assertCounts(2 to "Mandate", 0 to "PreludeCard", 6 to "ProjectCard")

      stdAction("HandleMandates") {
        assertCounts(8 to "ProjectCard", 1 to "PreludeCard")
        assertProds(0 to "M", 0 to "S", 0 to "T", 0 to "P", 0 to "E", 0 to "H")

        doTask("PlayCard<Class<PreludeCard>, Class<SocietySupport>>")
        assertProds(-1 to "M", 0 to "S", 0 to "T", 1 to "P", 1 to "E", 1 to "H")
      }
    }
  }
}
