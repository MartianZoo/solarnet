package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TerraformingMarsApi.Companion.tfm
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnmiTest {

  @Test
  fun unmi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    with(game.tfm(PLAYER1)) {
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
      sneak("14")
      assertCounts(14 to "Megacredit", 20 to "TR")

      // Do anything that raises TR, while we aren't even UNMI yet
      operations.initiate("UseAction1<AsteroidSP>")
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
