package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.production
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class RoboticWorkforceTest {

  @Test
  fun roboticWorkforce() {
    val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))

    with(game.session(PLAYER1)) {
      operation("4 ProjectCard, MassConverter, StripMine, IndustrialMicrobes")
      checkProduction(0, 3, 1, 0, 5, 0)

      game.session(PLAYER2).operation("ProjectCard, Mine")

      operation("RoboticWorkforce") {
        checkProduction(0, 3, 1, 0, 5, 0)

        Truth.assertThat(tasks.map { it.whyPending })
            .containsExactly(
                "CopyProductionBox<Player1, CardFront<Player1>(HAS BuildingTag<Player1>)> is abstract"
            )

        // This card has no building tag so it won't work
        assertThrows<NarrowingException>("1") { task("CopyProductionBox<MassConverter>") }

        // This card is someone else's (see what I did there)
        assertThrows<NarrowingException>("2") { task("CopyProductionBox<Mine<Player2>>") }

        // Obviously pretending it's mine is no help
        assertThrows<NarrowingException>("3") { task("CopyProductionBox<Mine>") }
        assertThrows<NarrowingException>("4") { task("CopyProductionBox<Mine<Player1>>") }

        task("CopyProductionBox<StripMine>")
      }
      checkProduction(0, 5, 2, 0, 3, 0)
    }
  }

  private fun PlayerSession.checkProduction(vararg exp: Int) =
      Truth.assertThat(production().values).containsExactlyElementsIn(exp.toList()).inOrder()
}
