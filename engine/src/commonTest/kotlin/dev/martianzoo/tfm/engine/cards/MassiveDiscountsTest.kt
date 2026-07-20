package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class MassiveDiscountsTest : CardTest() {

  @Test
  fun elCheapo() {
    val game = newGame("BRMVPX", 2)

    with(game.tfm(PLAYER1)) {
      phase("Action")
      sneak(
          "3, 2 ProjectCard, Steel, Titanium, AntiGravityTechnology, EarthCatapult, " +
              "ResearchOutpost, MassConverter, QuantumExtractor, Shuttles, SpaceStation, " +
              "AdvancedAlloys, 2 Phobolog, MercurianAlloys, RegoPlastics"
      ) // -1

      assertCounts(0 to "SpaceElevator", 3 to "M", 1 to "S", 1 to "T")

      playProject("SpaceElevator", 3, steel = 1, titanium = 1)
      assertCounts(1 to "SpaceElevator", 0 to "Resource")
    }
  }
}
