package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.phase
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import org.junit.jupiter.api.Test

class SupercapacitorsTest {
  @Test
  fun supercapactitors() {
    val game = Engine.newGame(GameSetup(Canon, "BRMX", 2))

    with(game.session(PLAYER1)) {
      phase("Action")
      writer.sneak("PROD[3 Energy, 5 Heat], 4 Energy, 100, 5 ProjectCard")

      assertProds(0 to "Megacredit")

      phase("Production")

      assertCounts(3 to "Energy")
      assertCounts(9 to "Heat")

      phase("Action")
      playCard("Supercapacitors", 4)

      operation("ProductionPhase FROM Phase") {
        matchTask("2 Heat FROM Energy!")
      }

      assertCounts(4 to "Energy")
      assertCounts(16 to "Heat")


//      assertCounts(
//          117 to "Megacredit", 0 to "Steel", 0 to "Titanium",
//          0 to "Plant", 3 to "Energy", 9 to "Heat")


    }
  }
}
