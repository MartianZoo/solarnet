package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

        tasks.extract { it.whyPending }.shouldContainExactlyInAnyOrder("abstract")

        // This card has no building tag so it won't work
        shouldThrow<NarrowingException> { doTask("CopyProductionBox<MassConverter>") }

        // This card is someone else's (see what I did there)
        shouldThrow<NarrowingException> { doTask("CopyProductionBox<Mine<Player2>>") }

        // Obviously pretending it's mine is no help
        shouldThrow<NarrowingException> { doTask("CopyProductionBox<Mine>") }

        shouldThrow<NarrowingException> { doTask("CopyProductionBox<Mine<Player1>>") }

        doTask("CopyProductionBox<StripMine>")
      }
      this.checkProduction(0, 5, 2, 0, 3, 0) // make annoying idea warning go away
    }
  }

  private fun TfmGameplay.checkProduction(vararg exp: Int) =
      production().values shouldBe exp.toList()
}
