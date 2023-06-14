package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ThorgateTest {
  @Test
  fun test() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("Thorgate", 10)
      godMode().sneak("-10")

      phase("Action")
      assertCounts(8 to "M", 1 to "Production<Class<E>>")
      stdProject("PowerPlantSP")
      assertCounts(0 to "M", 2 to "Production<Class<E>>")

      godMode().sneak("7")
      assertThrows<LimitsException> { stdProject("PowerPlantSP") }
    }
  }
}
