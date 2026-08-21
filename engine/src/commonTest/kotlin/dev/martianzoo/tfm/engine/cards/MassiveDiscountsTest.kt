package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class MassiveDiscountsTest : CardTest() {

  @Test
  fun `Stacks with other card discounts`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)

    engine.phase("Action")
    p1.manual(
        "5, 2 ProjectCard, Steel, Titanium, $AntiGravityTechnology, $EarthCatapult, " +
            "$ResearchOutpost, $MassConverter, $QuantumExtractor, $Shuttles, $SpaceStation, " +
            "$AdvancedAlloys, $Phobolog, $MercurianAlloys, $RegoPlastics"
    ) {
      doTask("CityTile<Tharsis_4_2>")
    }

    p1.playProject(SpaceElevator, 4, steel = 1, titanium = 1).expect("-4, -Steel, -Titanium")
  }
}
