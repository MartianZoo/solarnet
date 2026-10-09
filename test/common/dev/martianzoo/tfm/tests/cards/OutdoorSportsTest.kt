package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class OutdoorSportsTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can be played with an opponent's city beside an ocean`() {
    stan.exMachina("NormalCityTile<Stan, Tharsis_1_3>, OceanTile<Tharsis_1_2>")

    kim.playProject(OutdoorSports, 8).expect("PROD[2 MC]")
  }

  @Test
  internal fun `Cannot be played without city-ocean adjacency`() {
    stan.exMachina("NormalCityTile<Stan, Tharsis_1_3>")
    kim.exMachina("OceanTile<Tharsis_1_5>")

    shouldThrow<RequirementException> { kim.playProject(OutdoorSports, 8) }
  }
}
