package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.phase
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InsulationTest {

  @Test
  fun insulation_normal() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Teractor", 5)
      phase("Action")
      writer.unsafe().sneak("PROD[-1, 3 Heat]")
      assertProds(-1 to "M", 3 to "H")

      assertThrows<PetSyntaxException> { playCard("Insulation", 2, "PROD[0 Megacredit FROM Heat]") }
      assertThrows<PetSyntaxException> { playCard("Insulation", 2, "PROD[Ok]") }
      assertThrows<NarrowingException> { playCard("Insulation", 2, "Ok") }
      assertThrows<NarrowingException> {
        playCard("Insulation", 2, "PROD[2 Megacredit<P2> FROM Heat<P2>]")
      }

      playCard("Insulation", 2) {
        task("PROD[2 Megacredit FROM Heat]")
        assertProds(1 to "M", 1 to "H")
        rollItBack()
      }

      playCard("Insulation", 2) {
        task("PROD[Megacredit FROM Heat]")
        assertProds(0 to "M", 2 to "H")
        rollItBack()
      }
    }
  }
}
