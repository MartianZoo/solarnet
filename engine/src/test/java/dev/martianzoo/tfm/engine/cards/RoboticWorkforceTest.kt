package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoboticWorkforceTest {

  @Test
  fun roboticWorkforce() {
    val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))

    with(game.tfm(PLAYER1)) {
      godMode().manual("4 ProjectCard, MassConverter, StripMine, IndustrialMicrobes")
      checkProduction(0, 3, 1, 0, 5, 0)

      game.tfm(PLAYER2).godMode().manual("ProjectCard, Mine")

      godMode().manual("RoboticWorkforce") {
        checkProduction(0, 3, 1, 0, 5, 0)

        Truth.assertThat(tasks.extract { it.whyPending }).containsExactly("abstract")

        // This card has no building tag so it won't work
        assertThrows<NarrowingException>("1") { doTask("CopyProductionBox<MassConverter>") }

        // This card is someone else's (see what I did there)
        assertThrows<NarrowingException>("2") { doTask("CopyProductionBox<Mine<Player2>>") }

        // Obviously pretending it's mine is no help
        assertThrows<NarrowingException>("3") { doTask("CopyProductionBox<Mine>") }

        assertThrows<NarrowingException>("4") { doTask("CopyProductionBox<Mine<Player1>>") }

        doTask("CopyProductionBox<StripMine>")
      }
      this.checkProduction(0, 5, 2, 0, 3, 0) // make annoying idea warning go away
    }
  }

  private fun TfmGameplay.checkProduction(vararg exp: Int) =
      Truth.assertThat(production().values).containsExactlyElementsIn(exp.toList()).inOrder()
}
