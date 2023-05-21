package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.cardAction1
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TerraformingMars.production
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CanonCustomClassesTest {
  @Test
  fun robinsonMegacredit1() {
    val game = newGameForP1()
    with(game.session(PLAYER1)) {
      playCorp("RobinsonIndustries")
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
  fun robinsonMegacredit2() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.playCorp("RobinsonIndustries")
    p1.writer.unsafe().sneak("PROD[-1]")
    p1.checkProduction(-1, 0, 0, 0, 0, 0)

    p1.phase("Action")
    p1.cardAction1("RobinsonIndustries")
    p1.checkProduction(0, 0, 0, 0, 0, 0)
  }

  @Test
  fun robinsonNonMegacredit() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.playCorp("RobinsonIndustries")
    p1.writer.unsafe().sneak("PROD[1, S, P, E, H]")
    p1.checkProduction(1, 1, 0, 1, 1, 1)

    p1.phase("Action")
    p1.cardAction1("RobinsonIndustries")
    p1.checkProduction(1, 1, 1, 1, 1, 1)
  }

  @Test
  fun robinsonChoice() {
    val game = newGameForP1()
    with(game.session(PLAYER1)) {
      playCorp("RobinsonIndustries")
      writer.unsafe().sneak("PROD[S, P, E, H]")
      checkProduction(0, 1, 0, 1, 1, 1)

      phase("Action")
      cardAction1("RobinsonIndustries") {
        assertThat(tasks.map { it.instruction.toString() })
            .containsExactly(
                "Production<Player1, Class<Megacredit>>! OR Production<Player1, Class<Titanium>>!")
        task("PROD[1]")
        checkProduction(1, 1, 0, 1, 1, 1)
        rollItBack()
      }

      cardAction1("RobinsonIndustries", "PROD[T]") {
        checkProduction(0, 1, 1, 1, 1, 1)
        rollItBack()
      }

      assertThrows<NarrowingException> { cardAction1("RobinsonIndustries", "PROD[Steel]") }
    }
  }

  @Test
  fun roboticWorkforce() {
    val game = newGameForP1()
    val p1 = game.session(PLAYER1)

    p1.operation("4 ProjectCard, MassConverter, StripMine, IndustrialMicrobes")
    p1.checkProduction(0, 3, 1, 0, 5, 0)

    game.session(PLAYER2).operation("ProjectCard, Mine")

    p1.operation("RoboticWorkforce") {
      p1.checkProduction(0, 3, 1, 0, 5, 0)

      assertThat(tasks.map { it.whyPending }).containsExactly(
          "CopyProductionBox<Player1, CardFront<Player1>(HAS BuildingTag<Player1>)> is abstract"
      )

      // This card has no building tag so it won't work
      assertThrows<NarrowingException>("1") { p1.task("CopyProductionBox<MassConverter>") }
      p1.checkProduction(0, 3, 1, 0, 5, 0)

      // This card is someone else's (see what I did there)
      assertThrows<NarrowingException>("2") { p1.task("CopyProductionBox<Mine<Player2>>") }

      // Obviously pretending it's mine is no help
      assertThrows<NarrowingException>("3") { p1.task("CopyProductionBox<Mine>") }
      assertThrows<NarrowingException>("4") { p1.task("CopyProductionBox<Mine<Player1>>") }
      p1.checkProduction(0, 3, 1, 0, 5, 0)

      rollItBack()
    }

    p1.operation("RoboticWorkforce", "CopyProductionBox<StripMine>")
    p1.checkProduction(0, 5, 2, 0, 3, 0)
  }

  private fun newGameForP1() = Game.create(GameSetup(Canon, "BRMP", 2))

  private fun PlayerSession.checkProduction(vararg exp: Int) =
      assertThat(production().values).containsExactlyElementsIn(exp.toList()).inOrder()
}
