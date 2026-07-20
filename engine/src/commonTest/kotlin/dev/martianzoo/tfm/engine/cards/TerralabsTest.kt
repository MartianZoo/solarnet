package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class TerralabsTest : CardTest() {

  @Test
  fun terralabs() {
    val game = newGame("BMT", 2)
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("TerralabsResearch", 10)
      assertCounts(10 to "ProjectCard", 4 to "M")
      manual("4 BuyCard")
      assertCounts(14 to "ProjectCard", 0 to "M")
    }
  }
}
