package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LandClaimTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Prevents an opponent from building on the claimed area`() {
    kim.playProject(LandClaim, 1) { doTask("Community<Tharsis_1_1>") }

    shouldThrow<LimitsException> {
      stan.stdProject("CityProject") { placeTile(1, 1) }
    }
    kim.assertCounts(1 to "Community<Tharsis_1_1>", 0 to "Tile<Tharsis_1_1>")
  }

  @Test
  internal fun `Removes the claim when its owner builds there`() {
    kim.exMachina("Community<Tharsis_1_1>")

    kim.stdProject("GreeneryProject") { placeTile(1, 1) }.expect("-Community")
  }

  @Test
  internal fun `Prevents an opponent's Artificial Lake on the claimed area`() {
    kim.exMachina("Community<Tharsis_1_3>")
    kim.setToExMachina(12, "TemperatureStep")

    shouldThrow<LimitsException> {
      stan.playProject(ArtificialLake, 15) { placeTile(1, 3) }
    }
    kim.assertCounts(1 to "Community<Tharsis_1_3>", 0 to "OceanTile<Tharsis_1_3>")
  }

  @Test
  internal fun `Allows its owner's Artificial Lake on the claimed area`() {
    kim.exMachina("Community<Tharsis_1_3>")
    kim.setToExMachina(12, "TemperatureStep")

    kim.playProject(ArtificialLake, 15) { placeTile(1, 3) }.expect("OceanTile, -Community")
  }

  @Test
  internal fun `Keeps the claim when an unrelated tile is placed`() {
    kim.exMachina("Community<Tharsis_1_3>")

    kim.stdProject("CityProject") { placeTile(4, 2) }.expect("0 Community")
  }

  @Test
  internal fun `Does not establish greenery placement adjacency`() {
    kim.exMachina("Community<Tharsis_4_2>, NormalCityTile<Kim, Tharsis_1_1>")

    shouldThrow<NarrowingException> {
      kim.stdProject("GreeneryProject") { placeTile(4, 3) }
    }
    kim.assertCounts(1 to "Community<Tharsis_4_2>", 0 to "GreeneryTile<Tharsis_4_3>")
  }

  @Test
  internal fun `Allows greenery beside an owned tile while a distant claim remains`() {
    kim.exMachina("Community<Tharsis_4_2>, NormalCityTile<Kim, Tharsis_1_1>")

    kim.stdProject("GreeneryProject") { placeTile(2, 1) }.expect("GreeneryTile, 0 Community")
  }

  @Test
  internal fun `Enables distant greenery when an opposing claim blocks the last adjacent area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>, Community<Tharsis_2_2>")

    kim.stdProject("GreeneryProject") { placeTile(9, 7) }.expect("GreeneryTile, 0 Community<Stan>")
  }

  @Test
  internal fun `Prevents greenery on an opposing claim even when fallback is available`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>, Community<Tharsis_2_2>")

    shouldThrow<LimitsException> {
      kim.stdProject("GreeneryProject") { placeTile(2, 2) }
    }
    stan.assertCounts(1 to "Community<Tharsis_2_2>", 0 to "Tile<Tharsis_2_2>")
  }

  @Test
  internal fun `Does not enable distant greenery when the last adjacent area is an own claim`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>, Community<Tharsis_2_2>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>")

    shouldThrow<NarrowingException> {
      kim.stdProject("GreeneryProject") { placeTile(9, 7) }
    }
    kim.assertCounts(1 to "Community<Tharsis_2_2>", 0 to "Tile<Tharsis_9_7>")
  }

  @Test
  internal fun `Allows greenery on an own claim at the last adjacent area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>, Community<Tharsis_2_2>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>")

    kim.stdProject("GreeneryProject") { placeTile(2, 2) }.expect("GreeneryTile, -Community")
  }

  @Test
  internal fun `Does not enable distant greenery while other adjacent land remains available`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_2_2>")

    shouldThrow<NarrowingException> {
      kim.stdProject("GreeneryProject") { placeTile(9, 7) }
    }
    kim.assertCounts(0 to "Tile<Tharsis_9_7>")
  }

  @Test
  internal fun `Allows greenery on unclaimed adjacent land beside an opposing claim`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_2_2>")

    kim.stdProject("GreeneryProject") { placeTile(2, 1) }.expect("GreeneryTile, 0 Community<Stan>")
  }

  @Test
  internal fun `Cannot claim an occupied area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")

    shouldThrow<LimitsException> {
      kim.playProject(LandClaim, 1) { doTask("Community<Tharsis_1_1>") }
    }
    kim.assertCounts(0 to "Community<Tharsis_1_1>", 1 to "GreeneryTile<Tharsis_1_1>")
  }

  @Test
  internal fun `Cannot claim another player's reserved area`() {
    stan.exMachina("Community<Tharsis_1_3>")

    shouldThrow<LimitsException> {
      kim.playProject(LandClaim, 1) { doTask("Community<Tharsis_1_3>") }
    }
    stan.assertCounts(1 to "Community<Tharsis_1_3>")
    kim.assertCounts(0 to "Community<Tharsis_1_3>")
  }
}
