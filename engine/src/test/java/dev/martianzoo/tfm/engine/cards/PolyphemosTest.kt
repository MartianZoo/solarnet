package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.execapi.PlayerSession.Companion.session
import dev.martianzoo.tfm.execapi.TerraformingMars.cardAction1
import dev.martianzoo.tfm.execapi.TerraformingMars.phase
import dev.martianzoo.tfm.execapi.TerraformingMars.playCard
import dev.martianzoo.tfm.execapi.TerraformingMars.playCorp
import org.junit.jupiter.api.Test

class PolyphemosTest {

  @Test
  fun polyphemos() {
    val game = Engine.newGame(GameSetup(Canon, "BRMC", 2))
    with(game.session(PLAYER1)) {
      playCorp("Polyphemos", 10)
      assertCounts(10 to "ProjectCard", 0 to "M")

      phase("Action")
      writer.sneak("14")

      playCard("InventorsGuild", 9)
      assertCounts(9 to "ProjectCard", 5 to "M")

      cardAction1("InventorsGuild", "BuyCard")
      assertCounts(10 to "ProjectCard", 0 to "M")
    }
  }
}
