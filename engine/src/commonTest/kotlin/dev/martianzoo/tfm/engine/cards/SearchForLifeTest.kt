package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class SearchForLifeTest : CardTest() {
  @Test
  fun `a successful search is worth three points`() {
    val game = newGame(GameSetup(Canon, "BM", 2))
    val p1 = game.tfm(PLAYER1)

    p1.phase("Action")
    p1.godMode().manual("SearchForLife, 1")
    p1.cardAction1("SearchForLife") { doTask("Science<SearchForLife>") }

    game.tfm(ENGINE).phase("End")
    p1.assertCounts(23 to "VictoryPoint")
  }
}
