package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.UserException.InvalidReificationException
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Humanizing.production
import dev.martianzoo.tfm.engine.PlayerSession
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CanonCustomInstructionsTest {
  @Test
  fun robinsonMegacredit1() {
    val p1 = newGameForP1()

    p1.execute("CorporationCard, RobinsonIndustries")
    p1.execute("PROD[S, T, P, E, H]")
    checkProduction(p1, 0, 1, 1, 1, 1, 1)

    p1.execute("UseAction1<RobinsonIndustries>")
    assertThat(p1.agent.tasks()).isEmpty()
    checkProduction(p1, 1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonMegacredit2() {
    val p1 = newGameForP1()

    p1.execute("CorporationCard, RobinsonIndustries")
    p1.execute("PROD[-1]")
    checkProduction(p1, -1, 0, 0, 0, 0, 0)

    p1.execute("UseAction1<RobinsonIndustries>")
    checkProduction(p1, 0, 0, 0, 0, 0, 0)
  }

  @Test
  fun robinsonNonMegacredit() {
    val p1 = newGameForP1()

    p1.execute("CorporationCard, RobinsonIndustries")
    p1.execute("PROD[1, S, P, E, H]")
    checkProduction(p1, 1, 1, 0, 1, 1, 1)

    p1.execute("UseAction1<RobinsonIndustries>")
    checkProduction(p1, 1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonChoice() {
    val p1 = newGameForP1()

    p1.execute("CorporationCard, RobinsonIndustries")
    p1.execute("PROD[S, P, E, H]")
    checkProduction(p1, 0, 1, 0, 1, 1, 1)

    p1.initiate("UseAction1<RobinsonIndustries>")

    val cp = p1.game.checkpoint()
    p1.tryMatchingTask("PROD[1]")
    checkProduction(p1, 1, 1, 0, 1, 1, 1)

    p1.game.rollBack(cp)
    p1.tryMatchingTask("PROD[T]")
    checkProduction(p1, 0, 1, 1, 1, 1, 1)

    p1.game.rollBack(cp)
    assertThrows<InvalidReificationException> { p1.tryMatchingTask("PROD[Steel]") }
  }

  @Test
  fun roboticWorkforce() {
    val p1 = newGameForP1()

    p1.execute("5 ProjectCard")
    p1.execute("MassConverter")
    checkProduction(p1, 0, 0, 0, 0, 6, 0)

    p1.execute("StripMine")
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    p1.initiate("RoboticWorkforce")
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    // This card has no building tag so it won't work
    assertThrows<InvalidReificationException> { p1.tryMatchingTask("@copyProductionBox(MassConverter)") }
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    p1.tryMatchingTask("@copyProductionBox(StripMine)")
    p1.mustDrain()
    checkProduction(p1, 0, 4, 2, 0, 2, 0)
  }

  private fun newGameForP1(): PlayerSession {
    val setup = GameSetup(Canon, "BRMP", 2)
    return Engine.newGame(setup).asPlayer(PLAYER1).session()
  }

  private fun checkProduction(sess: PlayerSession, vararg exp: Int) {
    assertThat(sess.production().values).containsExactlyElementsIn(exp.toList()).inOrder()
  }
}
