package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class PristarTest : CardTest() {

  @Test
  fun pristar() {
    val game = newGame("BMPT", 2)
    val eng = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.assertCounts(0 to "Megacredit", 20 to "TR")
    p1.playCorp("Pristar", 0)
    p1.assertCounts(53 to "Megacredit", 18 to "TR")

    eng.phase("Prelude")
    p1.playPrelude("UnmiContractor")
    p1.assertCounts(53 to "Megacredit", 21 to "TR")

    eng.phase("Action")

    eng.phase("Production")
    p1.assertCounts(74 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.phase("Research") {
      p1.doTask("2 BuyCard")
      p2.doTask("2 BuyCard")
    }
    p1.assertCounts(68 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.phase("Action")
    eng.phase("Production")
    p1.assertCounts(95 to "Megacredit", 21 to "TR", 1 to "Preservation")
  }
}
