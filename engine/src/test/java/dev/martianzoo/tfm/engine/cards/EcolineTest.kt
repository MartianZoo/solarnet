package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EcolineTest {
  @Test
  fun test() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("Ecoline", 5)
      assertCounts(3 to "Plant")

      godMode().sneak("8 Plant")
      assertCounts(11 to "Plant")

      phase("Action")
      godMode().manual("CityTile<M52>")
      assertCounts(13 to "Plant")

      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<M42>") }
      assertCounts(7 to "Plant")

      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<M32>") }
      assertCounts(0 to "Plant")

      godMode().sneak("6 Plant")
      assertThrows<LimitsException> { stdAction("ConvertPlantsSA") }
    }
  }
}
