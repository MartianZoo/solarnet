package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.ApiUtils
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.UserException.InvalidReificationException
import dev.martianzoo.tfm.api.UserException.RequirementException
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
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

    // TODO why does autoExec break this?
    p1.execute("UseAction1<RobinsonIndustries>", autoExec = false)
    p1.doTask("-4! THEN @gainLowestProduction(Player1)")

    val cp = p1.game.checkpoint()
    p1.doTask("PROD[1]")
    checkProduction(p1, 1, 1, 0, 1, 1, 1)

    p1.game.rollBack(cp)
    p1.doTask("PROD[Titanium]") // TODO: sometimes we can't use shortnames
    checkProduction(p1, 0, 1, 1, 1, 1, 1)

    p1.game.rollBack(cp)
    assertThrows<InvalidReificationException> { p1.doTask("PROD[Steel]") }
  }

  @Test
  fun roboticWorkforce() {
    val p1 = newGameForP1()

    p1.execute("5 ProjectCard")
    p1.execute("MassConverter")
    checkProduction(p1, 0, 0, 0, 0, 6, 0)

    p1.execute("StripMine")
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    p1.execute("RoboticWorkforce")
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    // This card has no building tag so it won't work
    assertThrows<RequirementException> { p1.doTask("@copyProductionBox(MassConverter)") }
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    p1.doTask("@copyProductionBox(StripMine)")
    checkProduction(p1, 0, 4, 2, 0, 2, 0)

    // TODO: what if it wasn't a building card
  }

  private fun newGameForP1(): PlayerSession {
    val setup = GameSetup(Canon, "BRMP", 2)
    return Engine.newGame(setup).asPlayer(PLAYER1).session()
  }

  private fun checkProduction(sess: PlayerSession, vararg exp: Int) {
    val agent = sess.agent
    assertThat(ApiUtils.lookUpProductionLevels(agent.reader, agent.player).values)
        .containsExactlyElementsIn(exp.toList())
        .inOrder()
  }
}
