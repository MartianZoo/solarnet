package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.phase
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class MassiveDiscountsTest {

  @Test
  fun elCheapo() {
    val game = Engine.newGame(GameSetup(Canon, "BRMVPCX", 2))

    with(game.session(PLAYER1)) {
      phase("Action")
      operation("2 ProjectCard, Phobolog, Steel") // -1

      operation("AntiGravityTechnology, EarthCatapult")
      operation("ResearchOutpost", "CityTile<M33>")

      operation("MassConverter, QuantumExtractor, Shuttles, SpaceStation, WarpDrive")
      operation("AdvancedAlloys, MercurianAlloys, RegoPlastics")

      assertCounts(0 to "SpaceElevator", 23 to "M", 1 to "S", 10 to "T")

      playCard("SpaceElevator", 0, steel = 1, titanium = 1)
      assertCounts(1 to "SpaceElevator", 23 to "M", 0 to "S", 9 to "T")
    }
  }
}
