package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TerraformingMarsApi.Companion.tfm
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class TerralabsTest {

  @Test
  fun terralabs() {
    val game = Engine.newGame(GameSetup(Canon, "BMT", 2))
    val p1 = game.tfm(PLAYER1)

    p1.playCorp("TerralabsResearch", 10)
    p1.assertCounts(10 to "ProjectCard", 4 to "M")

    p1.operations.manual("4 BuyCard")
    p1.assertCounts(14 to "ProjectCard", 0 to "M")
  }
}
