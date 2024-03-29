package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnmiTest {

  @Test
  fun unmi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("UnitedNationsMarsInitiative", 0)
      assertCounts(40 to "Megacredit", 20 to "TR")

      phase("Action")

      assertThrows<RequirementException> { cardAction1("UnitedNationsMarsInitiative") }

      // Do anything that raises TR
      stdProject("AsteroidSP")
      assertCounts(26 to "Megacredit", 21 to "TR")

      cardAction1("UnitedNationsMarsInitiative")
      assertCounts(23 to "Megacredit", 22 to "TR")
    }
  }

  @Test
  fun unmiOutOfOrder() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      godMode().sneak("14")
      assertCounts(14 to "Megacredit", 20 to "TR")

      // Do anything that raises TR, while we aren't even UNMI yet
      godMode().manual("UseAction1<AsteroidSP>")
      assertCounts(0 to "Megacredit", 21 to "TR")

      playCorp("UnitedNationsMarsInitiative", 0)
      assertCounts(40 to "Megacredit", 21 to "TR")
      phase("Action")

      // The TR from earlier still counts
      cardAction1("UnitedNationsMarsInitiative")
      assertCounts(37 to "Megacredit", 22 to "TR")
    }
  }
}
