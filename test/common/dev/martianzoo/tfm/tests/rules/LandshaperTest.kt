package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class LandshaperTest : TfmSandboxTest() {
  // Unresolved adjudication (question 26, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3512556
  @Test
  internal fun `Capital and one greenery do not satisfy Landshaper`() {
    setupLandshaperCapital()
    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_05_02>",
      )
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Landshaper")
      }
    }
    kim.count("OwnedTile") shouldBe 2
    kim.count("Landshaper") shouldBe 0
  }

  // Unresolved adjudication (question 26, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3512556
  @Test
  internal fun `Capital and two greeneries do not satisfy Landshaper`() {
    setupLandshaperCapital()
    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_05_02>",
      )
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }
    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_06_01>",
      )
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Landshaper")
      }
    }
    kim.count("OwnedTile") shouldBe 3
    kim.count("Landshaper") shouldBe 0
  }

  // Unresolved adjudication (question 26, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3512556
  @Test
  internal fun `Capital and distinct city and greenery satisfy Landshaper`() {
    setupLandshaperCapital()
    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_05_02>",
      )
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }
    kim.inTurn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "NormalCityTile<Amazonis_01_01>",
          "Steel",
      )
    }

    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Landshaper")
    }
  }

  private fun setupLandshaperCapital() {
    newTestGame(addOptions = "AmazonisMap")
    kim.exMachina("$Capital, CapitalTile<$Capital, Amazonis_05_01>")
    kim.setToExMachina(80, "MC")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Landshaper")
      }
    }
  }
}
