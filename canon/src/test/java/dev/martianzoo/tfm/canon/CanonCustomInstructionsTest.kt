package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.Humanize.production
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CanonCustomInstructionsTest {
  @Test
  fun robinsonMegacredit1() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.action("CorporationCard, RobinsonIndustries")
    p1.action("PROD[S, T, P, E, H]")
    checkProduction(p1, 0, 1, 1, 1, 1, 1)

    p1.action("UseAction1<RobinsonIndustries>")
    assertThat(p1.tasks).isEmpty()
    checkProduction(p1, 1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonMegacredit2() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.action("CorporationCard, RobinsonIndustries")
    p1.action("PROD[-1]")
    checkProduction(p1, -1, 0, 0, 0, 0, 0)

    p1.action("UseAction1<RobinsonIndustries>")
    checkProduction(p1, 0, 0, 0, 0, 0, 0)
  }

  @Test
  fun robinsonNonMegacredit() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.action("CorporationCard, RobinsonIndustries")
    p1.action("PROD[1, S, P, E, H]")
    checkProduction(p1, 1, 1, 0, 1, 1, 1)

    p1.action("UseAction1<RobinsonIndustries>")
    checkProduction(p1, 1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonChoice() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.action("CorporationCard, RobinsonIndustries")
    p1.action("PROD[S, P, E, H]")
    checkProduction(p1, 0, 1, 0, 1, 1, 1)

    p1.action("UseAction1<RobinsonIndustries>") {
      doFirstTask("PROD[1]")
      checkProduction(p1, 1, 1, 0, 1, 1, 1)
      rollItBack()
    }

    p1.action("UseAction1<RobinsonIndustries>") {
      doFirstTask("PROD[T]")
      checkProduction(p1, 0, 1, 1, 1, 1, 1)
      rollItBack()
    }

    p1.action("UseAction1<RobinsonIndustries>") {
      assertThrows<NarrowingException> { doFirstTask("PROD[Steel]") }
      checkProduction(p1, 0, 1, 0, 1, 1, 1)
      rollItBack()
    }
  }

  @Test
  fun roboticWorkforce() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.action("3 ProjectCard, MassConverter, StripMine")
    checkProduction(p1, 0, 2, 1, 0, 4, 0)

    game.session(PLAYER2).action("ProjectCard, Mine")

    p1.action("RoboticWorkforce") {
      checkProduction(p1, 0, 2, 1, 0, 4, 0)
      // This card has no building tag so it won't work
      assertThrows<NarrowingException> { p1.doFirstTask("@copyProductionBox(MassConverter)") }
      checkProduction(p1, 0, 2, 1, 0, 4, 0)

      // This card is someone else's
      assertThrows<NarrowingException> { p1.doFirstTask("@copyProductionBox(Mine)") }
      assertThrows<NarrowingException> { p1.doFirstTask("@copyProductionBox(Mine<Player1>)") }
      assertThrows<NarrowingException> { p1.doFirstTask("@copyProductionBox(Mine<Player2>)") }
      checkProduction(p1, 0, 2, 1, 0, 4, 0)

      doFirstTask("@copyProductionBox(StripMine)")
      checkProduction(p1, 0, 4, 2, 0, 2, 0)
    }
  }

  private fun newGameForP1() = Game.create(GameSetup(Canon, "BRMP", 2))

  private fun checkProduction(sess: PlayerSession, vararg exp: Int) =
      assertThat(sess.production().values).containsExactlyElementsIn(exp.toList()).inOrder()
}
