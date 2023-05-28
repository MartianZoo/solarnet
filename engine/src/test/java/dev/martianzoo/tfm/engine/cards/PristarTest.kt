package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.execapi.PlayerSession.Companion.session
import dev.martianzoo.tfm.execapi.TerraformingMars.phase
import dev.martianzoo.tfm.execapi.TerraformingMars.playCorp
import dev.martianzoo.tfm.execapi.TerraformingMars.turn
import org.junit.jupiter.api.Test

class PristarTest {

  @Test
  fun pristar() {
    val game = Engine.newGame(GameSetup(Canon, "BMPT", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.assertCounts(0 to "Megacredit", 20 to "TR")

    p1.playCorp("Pristar", 0)
    p1.assertCounts(53 to "Megacredit", 18 to "TR")

    eng.phase("Prelude")
    p1.turn("UnmiContractor")
    p1.assertCounts(53 to "Megacredit", 21 to "TR")

    eng.phase("Action")
    eng.phase("Production")
    p1.assertCounts(74 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.operation("ResearchPhase FROM Phase") {
      p1.task("2 BuyCard")
      p2.task("2 BuyCard")
    }
    p1.assertCounts(68 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.phase("Action")
    eng.phase("Production")
    p1.assertCounts(95 to "Megacredit", 21 to "TR", 1 to "Preservation")
  }
}
