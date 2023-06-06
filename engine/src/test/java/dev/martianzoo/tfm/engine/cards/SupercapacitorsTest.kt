package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class SupercapacitorsTest {
  @Test
  fun supercapacitors() {
    val game = Engine.newGame(GameSetup(Canon, "BRMX", 2))

    val p1 = game.tfm(PLAYER1)
    with(p1) {
      phase("Action")
      godMode().sneak("PROD[3 Energy, 5 Heat], 4 Energy, 5 ProjectCard")

      assertProds(0 to "Megacredit")

      phase("Production")

      assertCounts(
          20 to "Megacredit", 0 to "Steel", 0 to "Titanium",
          0 to "Plant", 3 to "Energy", 9 to "Heat"
      )

      phase("Action")
      playProject("Supercapacitors", 4)

      phase("Production") {
        p1.doTask("2 Heat FROM Energy!")
      }

      assertCounts(
          37 to "Megacredit", 0 to "Steel", 0 to "Titanium",
          0 to "Plant", 4 to "Energy", 16 to "Heat"
      )
    }
  }
}
