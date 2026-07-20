package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class InsulationTest : CardTest() {

  @Test
  fun insulation_normal() {
    val game = newGame("BRM", 2)
    with(game.tfm(PLAYER1)) {
      phase("Action")
      sneak("100, 5 ProjectCard, PROD[-1, 3 Heat]")
      assertProds(-1 to "M", 3 to "H")

      playProject("Insulation", 2) {
        shouldThrow<PetSyntaxException> { doTask("PROD[0 Megacredit FROM Heat]") }
        shouldThrow<PetSyntaxException> { doFirstTask("PROD[Ok]") }
        shouldThrow<NarrowingException> { doFirstTask("Ok") }
        shouldThrow<NarrowingException> { doFirstTask("PROD[2 Megacredit<P2> FROM Heat<P2>]") }

        doTask("PROD[2 Megacredit FROM Heat]")
        assertProds(1 to "M", 1 to "H")
        abort()
      }

      playProject("Insulation", 2) { doFirstTask("PROD[Megacredit FROM Heat]") }
      assertProds(0 to "M", 2 to "H")
    }
  }
}
