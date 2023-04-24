package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ResourceUtils
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class CanonCustomInstructionsTest {
  @Test
  fun robinsonMegacredit1() {
    val p1 = newGameForP1()

    p1.execute("PROD[S, T, P, E, H]")
    checkProduction(p1, 0, 1, 1, 1, 1, 1)

    p1.execute("@gainLowestProduction(Player1)")
    checkProduction(p1, 1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonMegacredit2() {
    val p1 = newGameForP1()

    p1.execute("PROD[-1]")
    checkProduction(p1, -1, 0, 0, 0, 0, 0)

    p1.execute("@gainLowestProduction(Player1)")
    checkProduction(p1, 0, 0, 0, 0, 0, 0)
  }

  @Test
  fun robinsonNonMegacredit() {
    val p1 = newGameForP1()

    p1.execute("PROD[M, S, P, E, H]")
    checkProduction(p1, 1, 1, 0, 1, 1, 1)

    p1.execute("@gainLowestProduction(Player1)")
    checkProduction(p1, 1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonCant() {
    val p1 = newGameForP1()

    p1.execute("PROD[S, T, P, H]")
    checkProduction(p1, 0, 1, 1, 1, 0, 1)

    p1.execute("@gainLowestProduction(Player1)")
    checkProduction(p1, 0, 1, 1, 1, 0, 1)

    // TODO make better (reprodify?)
    assertThat(p1.agent.tasks().values.toStrings())
        .containsExactly(
            "A: [Player1] Production<Player1, Class<Megacredit>>! OR Production<Player1, " +
                "Class<Energy>>! (choice required in: `Production<Player1, Class<Megacredit>>! " +
                "OR Production<Player1, Class<Energy>>!`)")
  }

  @Test
  fun roboticWorkforce() {
    val p1 = newGameForP1()

    p1.execute("2 ProjectCard, PROD[5 E]")
    checkProduction(p1, 0, 0, 0, 0, 5, 0)

    p1.execute("StripMine")
    checkProduction(p1, 0, 2, 1, 0, 3, 0)

    p1.execute("RoboticWorkforce")
    checkProduction(p1, 0, 2, 1, 0, 3, 0)

    p1.doTask("@copyProductionBox(StripMine)")
    checkProduction(p1, 0, 4, 2, 0, 1, 0)

    // TODO: what if it wasn't a building card
  }

  private fun newGameForP1(): PlayerSession {
    val setup = GameSetup(Canon, "BRM", 2)
    return Engine.newGame(setup).asPlayer(PLAYER1).session()
  }

  private fun checkProduction(sess: PlayerSession, vararg exp: Int) {
    val agent = sess.agent
    assertThat(ResourceUtils.lookUpProductionLevels(agent.reader, agent.player).values)
        .containsExactlyElementsIn(exp.toList())
        .inOrder()
  }
}
