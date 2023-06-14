package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class PolyphemosTest {

  @Test
  fun polyphemos() {
    val game = Engine.newGame(GameSetup(Canon, "BRMC", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("Polyphemos", 7)

      phase("Action")
      assertCounts(7 to "ProjectCard", 15 to "M")

      playProject("InventorsGuild", 9)
      assertCounts(6 to "ProjectCard", 6 to "M")

      cardAction1("InventorsGuild") { doTask("BuyCard") }
      assertCounts(7 to "ProjectCard", 1 to "M")
    }
  }
}
