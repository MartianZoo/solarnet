package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TerraformingMars.sellPatents
import dev.martianzoo.tfm.engine.TerraformingMars.stdAction
import dev.martianzoo.tfm.engine.TerraformingMars.stdProject
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IndenturedWorkersTest {

  @Test
  fun indenturedWorkers() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Teractor", 6)

      phase("Action")
      playCard("IndenturedWorkers")

      // doing these things in between doesn't matter
      stdProject("AsteroidSP")
      writer.unsafe().sneak("9 Heat")
      stdAction("ConvertHeat")
      sellPatents(2)

      // we still have the discount
      playCard("EarthCatapult", 15)
      assertCounts(1 to "EarthCatapult")

      // but no more
      assertThrows<RequirementException> { playCard("AdvancedAlloys", 1) }
      playCard("AdvancedAlloys", 9)
      assertCounts(6 to "M")
    }
  }

  @Test
  fun indenturedWorkersGenerational() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Teractor", 10)

      phase("Action")
      playCard("IndenturedWorkers")
      operation("Generation")

      assertThrows<RequirementException> { playCard("AdvancedAlloys", 1) } // no diskey no morey
      playCard("AdvancedAlloys", 9)
    }
  }
}
