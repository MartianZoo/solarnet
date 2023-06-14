package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class MassiveDiscountsTest {

  @Test
  fun elCheapo() {
    val game = Engine.newGame(GameSetup(Canon, "BRMVPX", 2))

    with(game.tfm(PLAYER1)) {
      phase("Action")
      godMode().sneak("3, 2 ProjectCard, Phobolog, Steel, Titanium") // -1

      godMode().sneak("AntiGravityTechnology, EarthCatapult, ResearchOutpost")
      godMode().sneak("MassConverter, QuantumExtractor, Shuttles, SpaceStation")
      godMode().sneak("AdvancedAlloys, Phobolog, MercurianAlloys, RegoPlastics")

      assertCounts(0 to "SpaceElevator", 3 to "M", 1 to "S", 1 to "T")

      playProject("SpaceElevator", 3, steel = 1, titanium = 1)
      assertCounts(1 to "SpaceElevator", 0 to "Resource")
    }
  }
}
