package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class UnmiTest : CardTest() {

  @Test
  fun unmi() {
    val game = newGame(Canon.SIMPLE_GAME)
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("UnitedNationsMarsInitiative", 0)
      assertCounts(40 to "Megacredit", 20 to "TR")

      phase("Action")

      shouldThrow<RequirementException> { cardAction1("UnitedNationsMarsInitiative") }

      // Do anything that raises TR
      stdProject("AsteroidSP").expect("-14, TR")

      cardAction1("UnitedNationsMarsInitiative").expect("-3, TR")
    }
  }

  @Test
  fun unmiOutOfOrder() {
    val game = newGame(Canon.SIMPLE_GAME)
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      sneak("14")
      assertCounts(14 to "Megacredit", 20 to "TR")

      // Do anything that raises TR, while we aren't even UNMI yet
      manual("UseAction1<AsteroidSP>")
      assertCounts(0 to "Megacredit", 21 to "TR")

      playCorp("UnitedNationsMarsInitiative", 0)
      assertCounts(40 to "Megacredit", 21 to "TR")
      phase("Action")

      // The TR from earlier still counts
      cardAction1("UnitedNationsMarsInitiative").expect("-3, TR")
    }
  }
}
