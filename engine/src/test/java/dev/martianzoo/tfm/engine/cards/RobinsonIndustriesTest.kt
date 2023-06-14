package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.AbortOperationException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RobinsonIndustriesTest {
  val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))
  val p1 = game.tfm(PLAYER1)

  @BeforeEach
  fun setUp() {
    p1.phase("Corporation")
    p1.playCorp("RobinsonIndustries", 0)
    p1.phase("Action")
  }

  @Test
  fun megacredit1() {
    with(p1) {
      godMode().sneak("PROD[S, T, P, E, H]")
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 1 to "H")

      cardAction1("RobinsonIndustries")
      assertCounts(43 to "M")
      assertProds(1 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 1 to "H")
    }
  }

  @Test
  fun megacredit2() {
    with(p1) {
      godMode().sneak("PROD[-1]")
      assertProds(-1 to "M", 0 to "S", 0 to "T", 0 to "P", 0 to "E", 0 to "H")

      cardAction1("RobinsonIndustries")
      assertProds(0 to "M", 0 to "S", 0 to "T", 0 to "P", 0 to "E", 0 to "H")
    }
  }

  @Test
  fun nonMegacredit() {
    with(p1) {
      godMode().sneak("PROD[1, S, P, E, H]")
      assertProds(1 to "M", 1 to "S", 0 to "T", 1 to "P", 1 to "E", 1 to "H")
      cardAction1("RobinsonIndustries")
      assertProds(1 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 1 to "H")
    }
  }

  @Test
  fun choice() {
    with(game.tfm(PLAYER1)) {
      godMode().sneak("PROD[S, P, E, H]")
      assertProds(0 to "M", 1 to "S", 0 to "T", 1 to "P", 1 to "E", 1 to "H")

      cardAction1("RobinsonIndustries") {
        assertThat(tasks.extract { "${it.instruction}" })
            .containsExactly(
                "Production<Player1, Class<Megacredit>>! OR Production<Player1, Class<Titanium>>!")
        doTask("PROD[1]")
        assertProds(1 to "M", 1 to "S", 0 to "T", 1 to "P", 1 to "E", 1 to "H")
        throw AbortOperationException()
      }

      cardAction1("RobinsonIndustries") {
        doTask("PROD[T]")
        assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 1 to "H")
        throw AbortOperationException()
      }

      assertThrows<NarrowingException> {
        cardAction1("RobinsonIndustries") { doTask("PROD[Steel]") }
      }
    }
  }
}
