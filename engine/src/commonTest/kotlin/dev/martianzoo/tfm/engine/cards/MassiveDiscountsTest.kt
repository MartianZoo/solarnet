package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class MassiveDiscountsTest : CardTest() {

  @Test
  fun `with stacked discounts, plays Space Elevator`() {
    newGame("BMRVPX")

    engine.phase("Action")
    p1.manual(
        "5, 2 ProjectCard, Steel, Titanium, AntiGravityTechnology, EarthCatapult, " +
            "ResearchOutpost, MassConverter, QuantumExtractor, Shuttles, SpaceStation, " +
            "AdvancedAlloys, Phobolog, MercurianAlloys, RegoPlastics"
    ) {
      doTask("CityTile<M42>")
    }

    p1.playProject("SpaceElevator", 4, steel = 1, titanium = 1)
        .expect("SpaceElevator, -4 Megacredit, -Steel, -Titanium")
  }
}
