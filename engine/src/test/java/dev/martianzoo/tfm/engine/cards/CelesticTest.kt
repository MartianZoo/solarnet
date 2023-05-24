package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.pass
import dev.martianzoo.tfm.engine.TerraformingMars.phase
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TerraformingMars.stdAction
import dev.martianzoo.tfm.engine.TerraformingMars.stdProject
import dev.martianzoo.tfm.engine.TerraformingMars.turn
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CelesticTest {
  @Test
  fun celestic() {
    val game = Engine.newGame(GameSetup(Canon, "BRMV", 2))
    with(game.session(PLAYER1)) {
      playCorp("Celestic", 5)
      assertCounts(5 to "ProjectCard", 27 to "M")

      phase("Action")
      assertThrows<NarrowingException> { playCard("Mine") }
      assertThrows<NarrowingException> { stdProject("Aquifer") }
      assertThrows<NarrowingException> { stdAction("ConvertPlants") }

      pass()

      phase("Production")
      operation("ResearchPhase FROM Phase") {
        task("2 BuyCard")
        asPlayer(PLAYER2).task("2 BuyCard")
      }
      phase("Action")
      assertThrows<NarrowingException> { playCard("Mine") }

      assertCounts(1 to "Mandate")
      assertCounts(7 to "ProjectCard")
      turn("UseAllMandates")
      assertCounts(9 to "ProjectCard")
      playCard("Mine", 4)
    }
  }
}
