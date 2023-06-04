package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TerraformingMarsApi
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CelesticTest {
  @Test
  fun celestic() {
    val game = Engine.newGame(GameSetup(Canon, "BRMV", 2))
    val p1 = TerraformingMarsApi(game, PLAYER1)
    val p2 = TerraformingMarsApi(game, PLAYER2)

    with(p1) {
      playCorp("Celestic", 5)
      assertCounts(5 to "ProjectCard", 27 to "M")

      phase("Action")
      assertThrows<NarrowingException> { playCard("Mine") }
      assertThrows<NarrowingException> { stdProject("Aquifer") }
      assertThrows<NarrowingException> { stdAction("ConvertPlants") }

      pass()

      phase("Production")
      this.turns.operationLayer().initiate("ResearchPhase FROM Phase") {
        doFirstTask("2 BuyCard")
        p2.turns.doTask("2 BuyCard")
      }
      phase("Action")
      assertThrows<NarrowingException> { playCard("Mine") }

      assertCounts(1 to "Mandate")
      assertCounts(7 to "ProjectCard")
      this.turns.turn { doTask("UseAllMandates") }
      assertCounts(9 to "ProjectCard")
      playCard("Mine", 4)
    }
  }
}
