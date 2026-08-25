package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class OutdoorSportsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("8, ProjectCard")
  }

  @Test
  internal fun `Can be played with an opponent's city beside an ocean`() {
    requireP2().manual("CityTile<Tharsis_1_3>, OceanTile<Tharsis_1_2>")
    p1.playProject(OutdoorSports, 8).expect("PROD[2 Megacredit]")
  }

  @Test
  internal fun `Cannot be played without city-ocean adjacency`() {
    requireP2().manual("CityTile<Tharsis_1_3>")
    p1.manual("OceanTile<Tharsis_1_5>")
    shouldThrow<RequirementException> { p1.playProject(OutdoorSports, 8) }
  }
}
