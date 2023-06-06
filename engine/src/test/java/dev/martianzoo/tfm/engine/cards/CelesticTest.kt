package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CelesticTest {
  @Test
  fun celestic() {
    val game = Engine.newGame(GameSetup(Canon, "BRMV", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    with(p1) {
      playCorp("Celestic", 5)
      assertCounts(5 to "ProjectCard", 27 to "M")
      godMode().sneak("100, 10 Heat")

      phase("Action")
      assertThrows<RequirementException> { playProject("Mine") }
      assertThrows<RequirementException> { stdProject("AsteroidSP") }
      assertThrows<RequirementException> { stdAction("ConvertHeat") }

      pass()

      phase("Production")
      phase("Research") {
        p1.doTask("2 BuyCard")
        p2.doTask("2 BuyCard")
      }
      phase("Action")
      assertThrows<RequirementException> { playProject("Mine") }

      assertCounts(1 to "Mandate")
      assertCounts(7 to "ProjectCard")
      stdAction("HandleMandates")
      assertCounts(9 to "ProjectCard")
      playProject("Mine", 4)
    }
  }
}
