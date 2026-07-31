package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class OutdoorSportsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame("PromoCardPack")
    engine.phase("Action")
    p1.manual("8, ProjectCard")
  }

  @Test
  fun `with a p2 city beside an ocean, plays Outdoor Sports`() {
    requireP2().manual("CityTile<Tharsis_1_3>, OceanTile<Tharsis_1_2>")
    p1.playProject("OutdoorSports", 8).expect("PROD[2 Megacredit]")
  }

  @Test
  fun `without city-ocean adjacency, tries to play Outdoor Sports`() {
    requireP2().manual("CityTile<Tharsis_1_3>")
    p1.manual("OceanTile<Tharsis_1_5>")
    shouldThrow<RequirementException> { p1.playProject("OutdoorSports", 8) }
  }
}
