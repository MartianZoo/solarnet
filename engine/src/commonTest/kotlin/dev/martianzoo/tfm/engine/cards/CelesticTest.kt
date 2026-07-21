package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CelesticTest : CardTest() {
  @Test
  fun celestic() {
    val game = newGame("BRMV", 2)
    with(game.tfm(PLAYER1)) {
      playCorp("Celestic", 5)
      assertCounts(5 to "ProjectCard", 27 to "M")
      sneak("100, 10 Heat")

      phase("Action")
      shouldThrow<RequirementException> { playProject("Mine", 4) }
      shouldThrow<RequirementException> { stdProject("AsteroidSP") }
      shouldThrow<RequirementException> { stdAction("ConvertHeatSA") }

      pass()

      asActor(ENGINE).nextGeneration(2, 2)

      shouldThrow<RequirementException> { playProject("Mine", 4) }

      assertCounts(1 to "Mandate")
      assertCounts(7 to "ProjectCard")
      stdAction("HandleMandates")
      assertCounts(9 to "ProjectCard")
      playProject("Mine", 4)
    }
  }
}
