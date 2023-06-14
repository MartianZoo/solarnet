package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.AbortOperationException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InsulationTest {

  @Test
  fun insulation_normal() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("Ecoline", 5)

      phase("Action")
      godMode().sneak("PROD[-1, 3 Heat]")
      assertProds(-1 to "M", 3 to "H")

      playProject("Insulation", 2) {
        assertThrows<PetSyntaxException> { doTask("PROD[0 Megacredit FROM Heat]") }
        assertThrows<PetSyntaxException> { doFirstTask("PROD[Ok]") }
        assertThrows<NarrowingException> { doFirstTask("Ok") }
        assertThrows<NarrowingException> { doFirstTask("PROD[2 Megacredit<P2> FROM Heat<P2>]") }

        doTask("PROD[2 Megacredit FROM Heat]")
        assertProds(1 to "M", 1 to "H")

        throw AbortOperationException() // TODO a way to roll back without aborting?
      }

      playProject("Insulation", 2) { doFirstTask("PROD[Megacredit FROM Heat]") }
      assertProds(0 to "M", 2 to "H")
    }
  }
}
