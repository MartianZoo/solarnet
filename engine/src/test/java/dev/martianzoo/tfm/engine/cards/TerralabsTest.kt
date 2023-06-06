package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class TerralabsTest {

  @Test
  fun terralabs() {
    val game = Engine.newGame(GameSetup(Canon, "BMT", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("TerralabsResearch", 10)
      assertCounts(10 to "ProjectCard", 4 to "M")
      godMode().manual("4 BuyCard")
      assertCounts(14 to "ProjectCard", 0 to "M")
    }
  }
}
