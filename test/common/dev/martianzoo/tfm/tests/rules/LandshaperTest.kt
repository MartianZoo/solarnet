package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class LandshaperTest : TfmSandboxTest() {
  // Resolved FAQ: the city, greenery, and special tile must be three distinct tiles.
  // Earlier question:
  // https://boardgamegeek.com/thread/3512556/article/46088087#46088087
  @Test
  internal fun `Capital and one greenery do not satisfy Landshaper`() {
    setupLandshaperCapital()
    kim.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Amazonis_05_02>")
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Landshaper>>")
        doTask("-8 MC")
        doTask("Landshaper")
      }
    }
    kim.count("OwnedTile") shouldBe 2
    kim.count("Landshaper") shouldBe 0
  }

  @Test
  internal fun `Capital and two greeneries do not satisfy Landshaper`() {
    setupLandshaperCapital()
    kim.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Amazonis_05_02>")
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }
    kim.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Amazonis_06_01>")
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Landshaper>>")
        doTask("-8 MC")
        doTask("Landshaper")
      }
    }
    kim.count("OwnedTile") shouldBe 3
    kim.count("Landshaper") shouldBe 0
  }

  @Test
  internal fun `Capital and distinct city and greenery satisfy Landshaper`() {
    setupLandshaperCapital()
    kim.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Amazonis_05_02>")
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }
    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "-25 MC",
          "PROD[MC]",
          "NormalCityTile<Amazonis_01_01>",
          "Steel",
      )
    }

    kim.inTurn {
      doTasks("ClaimMilestone<Class<Landshaper>>", "-8 MC")
      doTask("Landshaper")
    }
  }

  private fun setupLandshaperCapital() {
    newTestGame(addOptions = "AmazonisMap")
    kim.exMachina("$Capital, CapitalTile<$Capital, Amazonis_05_01>")
    kim.setToExMachina(80, "MC")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Landshaper>>")
        doTask("-8 MC")
        doTask("Landshaper")
      }
    }
  }
}
