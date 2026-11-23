package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TilePlacingTest : TfmSandboxTest() {
  @Test
  internal fun `A city cannot be placed next to another city`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Tharsis_4_6>, NormalCityTile<Tharsis_4_4>")
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "CityTile<Tharsis_3_4>",
        )
      }
    }
  }

  @Test
  internal fun `A greenery cannot cover an existing city`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Tharsis_3_3>")
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            *standardProjectTasks("GreeneryProject"),
            "23 Pay<> FROM MC",
            "GreeneryTile<Tharsis_3_3>",
        )
      }
    }
  }

  @Test
  internal fun `A player whose tiles are surrounded may place greenery elsewhere`() {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_4_3>")
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            *standardProjectTasks("GreeneryProject"),
            "23 Pay<> FROM MC",
            "GreeneryTile<Tharsis_7_5>",
        )
      }
    }
    stan.exMachina(
        "GreeneryTile<Tharsis_3_2>, GreeneryTile<Tharsis_3_3>, " +
            "GreeneryTile<Tharsis_4_2>, GreeneryTile<Tharsis_4_4>"
    )
    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_7_5>",
          "OxygenStep",
          "TerraformRating",
      )
    }
  }

  @Test
  internal fun `An unaffordable south pole does not permit greenery elsewhere`() {
    newTestGame("HellasMap")
    kim.exMachina("GreeneryTile<Hellas_9_8>")
    stan.exMachina("GreeneryTile<Hellas_8_7>, GreeneryTile<Hellas_8_8>, GreeneryTile<Hellas_9_9>")
    kim.setToExMachina(8, "Plant")
    kim.setToExMachina(4, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Hellas_9_7>",
            "OxygenStep",
            "TerraformRating",
            "OceanTile<Hellas_4_6>",
            "Plant",
            "TerraformRating",
            "-6 MC",
        )
      }
    }
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Hellas_7_5>",
        )
      }
    }

    // The bonus beside this ocean supplies the missing 2 MC for the south-pole charge.
    agents.correct("OceanTile<Hellas_5_6>")
    kim.inTurn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_9_7>",
          "OxygenStep",
          "TerraformRating",
          "OceanTile<Hellas_4_6>",
          "Plant",
          "TerraformRating",
          "2 MC",
          "-6 MC",
      )
    }
  }

  @Test
  internal fun `Greenery must be next to the players tile rather than an opponents`() {
    prepareCityNeighbours()
    kim.inTurn {
      doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_6_4>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_6_5>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_6_6>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_7_4>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_7_7>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_8_4>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_8_8>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_9_5>") }
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Tharsis_9_8>") }
      doTasks("GreeneryTile<Tharsis_9_7>", "OxygenStep", "TerraformRating")
    }
  }

  @Test internal fun `Greenery may go northwest of an owned city`() = placeBesideCity("Tharsis_7_5")

  @Test internal fun `Greenery may go northeast of an owned city`() = placeBesideCity("Tharsis_7_6")

  @Test internal fun `Greenery may go west of an owned city`() = placeBesideCity("Tharsis_8_5")

  @Test
  internal fun `Greenery may go east of an owned city`() =
      placeBesideCity("Tharsis_8_7", "ProjectCard")

  @Test
  internal fun `Greenery may go southwest of an owned city`() =
      placeBesideCity("Tharsis_9_6", "2 Steel")

  @Test internal fun `Greenery may go southeast of an owned city`() = placeBesideCity("Tharsis_9_7")

  private fun prepareCityNeighbours() {
    newTestGame()
    kim.exMachina("NormalCityTile<Tharsis_8_6>")
    stan.exMachina("NormalCityTile<Tharsis_6_7>")
  }

  private fun placeBesideCity(area: String, bonus: String? = null) {
    prepareCityNeighbours()
    kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<$area>",
          "OxygenStep",
          "TerraformRating",
      )
      if (bonus != null) doTask(bonus)
    }
  }

  @Test
  internal fun `Amazonis placement can fund Reds but choosing steel leaves the action unpaid`() {
    newTestGame(addOptions = "AmazonisMap, TurmoilExpansion")
    // The correction changes government after ActionPhase began, so include its policy.
    kim.exMachina("Ruling<Reds FROM Greens>, RedsPolicy, 8 Plant")
    kim.setToExMachina(2, "MC")
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Amazonis_05_03>",
            "Steel",
            "OxygenStep",
            "TerraformRating",
            "-3 MC",
        )
      }
    }
    kim.count("GreeneryTile") shouldBe 0
    kim.count("Plant") shouldBe 8
    kim.count("MC") shouldBe 2
    kim.count("Steel") shouldBe 0

    kim.inTurn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_03>",
          "MC",
          "OxygenStep",
          "TerraformRating",
          "-3 MC",
      )
    }
  }
}
