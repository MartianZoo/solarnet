package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.cardAction1
import dev.martianzoo.tfm.engine.TerraformingMars.phase
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TerraformingMars.production
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RobinsonIndustriesTest {
  val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))

  @Test
  fun megacredit1() {
    with(game.session(PLAYER1)) {
      playCorp("RobinsonIndustries", 0)
      assertThat(count("Megacredit")).isEqualTo(47)

      writer.unsafe().sneak("PROD[S, T, P, E, H]")
      checkProduction(0, 1, 1, 1, 1, 1)

      phase("Action")
      cardAction1("RobinsonIndustries")
      assertThat(count("Megacredit")).isEqualTo(43)
      checkProduction(1, 1, 1, 1, 1, 1)
    }
  }

  @Test
  fun megacredit2() {
    val p1 = game.session(PLAYER1)

    p1.playCorp("RobinsonIndustries", 0)
    p1.writer.unsafe().sneak("PROD[-1]")
    p1.checkProduction(-1, 0, 0, 0, 0, 0)

    p1.phase("Action")
    p1.cardAction1("RobinsonIndustries")
    p1.checkProduction(0, 0, 0, 0, 0, 0)
  }

  @Test
  fun nonMegacredit() {
    val p1 = game.session(PLAYER1)

    p1.playCorp("RobinsonIndustries", 0)
    p1.writer.unsafe().sneak("PROD[1, S, P, E, H]")
    p1.checkProduction(1, 1, 0, 1, 1, 1)

    p1.phase("Action")
    p1.cardAction1("RobinsonIndustries")
    p1.checkProduction(1, 1, 1, 1, 1, 1)
  }

  @Test
  fun choice() {
    with(game.session(PLAYER1)) {
      playCorp("RobinsonIndustries", 0)
      writer.unsafe().sneak("PROD[S, P, E, H]")
      checkProduction(0, 1, 0, 1, 1, 1)

      phase("Action")
      cardAction1("RobinsonIndustries") {
        assertThat(tasks.extract { "${it.instruction}" })
            .containsExactly(
                "Production<Player1, Class<Megacredit>>! OR Production<Player1, Class<Titanium>>!")
        task("PROD[1]")
        checkProduction(1, 1, 0, 1, 1, 1)
        abortAndRollBack()
      }

      cardAction1("RobinsonIndustries", "PROD[T]") {
        checkProduction(0, 1, 1, 1, 1, 1)
        abortAndRollBack()
      }

      assertThrows<NarrowingException> { cardAction1("RobinsonIndustries", "PROD[Steel]") }
    }
  }

  private fun PlayerSession.checkProduction(vararg exp: Int) =
      assertThat(production().values).containsExactlyElementsIn(exp.toList()).inOrder()
}
