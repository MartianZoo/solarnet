package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ThorgateTest : CardTest() {
  @Test
  fun test() {
    val game = newGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      playCorp("Thorgate", 10)
      sneak("-10")

      phase("Action")
      stdProject("PowerPlantSP").expect("-8, PROD[Energy]")

      sneak("7")
      shouldThrow<LimitsException> { stdProject("PowerPlantSP") }
    }
  }
}
