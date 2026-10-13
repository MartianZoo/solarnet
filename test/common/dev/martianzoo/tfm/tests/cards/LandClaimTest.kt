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
    kim.inTurn {
      doTasks("PlayProject<Class<LandClaim>>", "-1 MC", "LandClaim FROM ProjectCard")
      selectOnlyTask()
      doTask("Community<Tharsis_1_1>")
    }

    shouldThrow<LimitsException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<CityProject>")
        selectTask("-X? MC")
        doTask("-25 MC")
        selectTask("PROD[MC]")
        selectOnlyTask()
        doTask("CityTile<Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "Community<Tharsis_1_1>", 0 to "Tile<Tharsis_1_1>")
  }

  @Test
  internal fun `Removes the claim when its owner builds there`() {
    kim.exMachina("Community<Tharsis_1_1>")

    kim.inTurn {
          doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_1_1>")
          selectTask("OxygenStep*")
          doTasks("2 Steel", "TerraformRating")
        }
        .expect("-Community")
  }

  @Test
  internal fun `Prevents an opponent's Artificial Lake on the claimed area`() {
    kim.exMachina("Community<Tharsis_1_3>")
    kim.setToExMachina(12, "TemperatureStep")

    shouldThrow<LimitsException> {
      stan.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
        selectTask("-X? Steel")
        selectTask("Ok")
        doTask("ArtificialLake FROM ProjectCard")
        selectOnlyTask()
        doTask("OceanTile<Tharsis_1_3>")
      }
    }
    kim.assertCounts(1 to "Community<Tharsis_1_3>", 0 to "OceanTile<Tharsis_1_3>")
  }

  @Test
  internal fun `Allows its owner's Artificial Lake on the claimed area`() {
    kim.exMachina("Community<Tharsis_1_3>")
    kim.setToExMachina(12, "TemperatureStep")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<$ArtificialLake>>",
              "-15 MC",
              "Ok", // No steel paid.
              "$ArtificialLake FROM ProjectCard",
              "OceanTile<Tharsis_1_3>",
              "TerraformRating",
          )
        }
        .expect("-Community")
  }

  @Test
  internal fun `Keeps the claim when an unrelated tile is placed`() {
    kim.exMachina("Community<Tharsis_1_3>")

    kim.inTurn {
          doTasks(
              "UseStandardProject<CityProject>",
              "-25 MC",
              "PROD[MC]",
              "CityTile<Tharsis_4_2>",
              "Plant",
          )
        }
        .expect("0 Community")
  }

  @Test
  internal fun `Does not establish greenery placement adjacency`() {
    kim.exMachina("Community<Tharsis_4_2>, NormalCityTile<Kim, Tharsis_1_1>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<GreeneryProject>")
        selectTask("-X? MC")
        doTask("-23 MC")
        selectOnlyTask()
        doTask("GreeneryTile<Tharsis_4_3>")
      }
    }
    kim.assertCounts(1 to "Community<Tharsis_4_2>", 0 to "GreeneryTile<Tharsis_4_3>")
  }

  @Test
  internal fun `Allows greenery beside an owned tile while a distant claim remains`() {
    kim.exMachina("Community<Tharsis_4_2>, NormalCityTile<Kim, Tharsis_1_1>")

    kim.inTurn {
          doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_2_1>")
          selectTask("OxygenStep*")
          doTask("TerraformRating")
        }
        .expect("0 Community")
  }

  @Test
  internal fun `Enables distant greenery when an opposing claim blocks the last adjacent area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>, Community<Tharsis_2_2>")

    kim.inTurn {
          doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_9_7>")
          selectTask("OxygenStep*")
          doTask("TerraformRating")
        }
        .expect("0 Community<Stan>")
  }

  @Test
  internal fun `Prevents greenery on an opposing claim even when fallback is available`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>, Community<Tharsis_2_2>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<GreeneryProject>")
        selectTask("-X? MC")
        doTask("-23 MC")
        selectOnlyTask()
        doTask("GreeneryTile<Tharsis_2_2>")
      }
    }
    stan.assertCounts(1 to "Community<Tharsis_2_2>", 0 to "Tile<Tharsis_2_2>")
  }

  @Test
  internal fun `Does not enable distant greenery when the last adjacent area is an own claim`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>, Community<Tharsis_2_2>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<GreeneryProject>")
        selectTask("-X? MC")
        doTask("-23 MC")
        selectOnlyTask()
        doTask("GreeneryTile<Tharsis_9_7>")
      }
    }
    kim.assertCounts(1 to "Community<Tharsis_2_2>", 0 to "Tile<Tharsis_9_7>")
  }

  @Test
  internal fun `Allows greenery on an own claim at the last adjacent area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>, Community<Tharsis_2_2>")
    stan.exMachina("NormalCityTile<Stan, Tharsis_2_1>")

    kim.inTurn {
          doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_2_2>")
          selectTask("OxygenStep*")
          doTasks("Steel", "TerraformRating")
        }
        .expect("-Community")
  }

  @Test
  internal fun `Does not enable distant greenery while other adjacent land remains available`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_2_2>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<GreeneryProject>")
        selectTask("-X? MC")
        doTask("-23 MC")
        selectOnlyTask()
        doTask("GreeneryTile<Tharsis_9_7>")
      }
    }
    kim.assertCounts(0 to "Tile<Tharsis_9_7>")
  }

  @Test
  internal fun `Allows greenery on unclaimed adjacent land beside an opposing claim`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_2_2>")

    kim.inTurn {
          doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_2_1>")
          selectTask("OxygenStep*")
          doTask("TerraformRating")
        }
        .expect("0 Community<Stan>")
  }

  @Test
  internal fun `Cannot claim an occupied area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_1>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<LandClaim>>")
        selectTask("-X? MC")
        doTasks("-1 MC", "LandClaim FROM ProjectCard")
        selectOnlyTask()
        doTask("Community<Tharsis_1_1>")
      }
    }
    kim.assertCounts(0 to "Community<Tharsis_1_1>", 1 to "GreeneryTile<Tharsis_1_1>")
  }

  @Test
  internal fun `Cannot claim another player's reserved area`() {
    stan.exMachina("Community<Tharsis_1_3>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<LandClaim>>")
        selectTask("-X? MC")
        doTasks("-1 MC", "LandClaim FROM ProjectCard")
        selectOnlyTask()
        doTask("Community<Tharsis_1_3>")
      }
    }
    stan.assertCounts(1 to "Community<Tharsis_1_3>")
    kim.assertCounts(0 to "Community<Tharsis_1_3>")
  }
}
