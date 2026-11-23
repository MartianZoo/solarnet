package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EndgameRulesTest : TfmGameplayTest() {
  // Omit unrelated production and colony choices; each scenario supplies its relevant position.
  private val options = "-QuickStartVariant, -ColoniesExpansion"

  @Test
  internal fun `Final production supplies plants before final greenery without further terraforming`() {
    newTestGame("$options, -VenusNextExpansion", playerCount = 2)
    // The earlier resource collection and terraforming are incidental to the final phase order.
    kim.exMachina("7 Plant, PROD[Plant]")
    agents.correct(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>"
    )

    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("20 MC", "Plant")
    stan.doTask("20 MC")
    kim.completeOperation {
          doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Tharsis_3_3>",
          )
        }
        .expect("0 OxygenStep, 0 TerraformRating")
    kim.doTask("Ok") // No further final greenery.
    stan.doTask("Ok") // No final greenery.

    kim.count("VictoryPoint") shouldBe 0
    stan.count("VictoryPoint") shouldBe 0
    kim.count("Victory") shouldBe 0
    kim.doTask("21 VictoryPoint")
    stan.doTask("20 VictoryPoint")
    kim.count("VictoryPoint") shouldBe 21
    stan.count("VictoryPoint") shouldBe 20
    kim.count("Victory") shouldBe 1
  }

  @Test
  internal fun `MC breaks an otherwise tied multiplayer final score`() {
    newTestGame("$options, -VenusNextExpansion", playerCount = 2)
    // Equal corporations and resources give a tied score; the patent sale supplies the tiebreaker.
    agents.correct(
        "-GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>"
    )
    kim.turn { doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard") }
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("Ok") // No final greenery.
    stan.doTask("Ok") // No final greenery.

    kim.doTask("20 VictoryPoint")
    stan.doTask("20 VictoryPoint")
    kim.count("VictoryPoint") shouldBe 20
    stan.count("VictoryPoint") shouldBe 20
    kim.count("MC") shouldBe 63
    stan.count("MC") shouldBe 62
    kim.count("Victory") shouldBe 1
    stan.count("Victory") shouldBe 0
  }

  @Test
  internal fun `Mandatory Venus keeps multiplayer going after the base parameters are complete`() {
    newTestGame("$options, MandatoryVenusVariant", playerCount = 2)
    // Prepare the base end conditions and leave Venus a step short; earlier terraforming is
    // incidental.
    agents.correct(
        "GpComplete<Class<TemperatureStep>>, GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, -GpGameEndBarrier<Class<TemperatureStep>>, " +
            "-GpGameEndBarrier<Class<OxygenStep>>, -GpGameEndBarrier<Class<OceanTile>>, 14 VenusStep"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    admin.count("SolarPhase") shouldBe 1

    // Real World Government terraforming clears the remaining end condition.
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("Generation") shouldBe 2
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    stan.doTask("Ok") // No final greenery.
    kim.doTask("Ok") // No final greenery.
    players.forEach { it.doTask("20 VictoryPoint") }
    kim.count("Victory") shouldBe 1
    stan.count("Victory") shouldBe 1
  }

  @Test
  internal fun `An unfinished standard solo game loses after fourteen generations`() {
    newTestGame("$options, -VenusNextExpansion", playerCount = 1)
    // Here the countdown itself is under test, so exercise every generation.
    repeat(13) {
      kim.doTask("Pass")
      kim.doTasks("14 MC", "4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    }
    kim.doTask("Pass")
    kim.doTask("14 MC")
    admin.count("Generation") shouldBe 14
    kim.count("Victory") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Prelude shortens the standard solo game to twelve generations`() {
    newTestGame("$options, PreludeExpansion, -VenusNextExpansion", playerCount = 1)
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "$Donation FROM PreludeCard",
        "21 MC",
    )

    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
        "$AlliedBank FROM PreludeCard",
        "PROD[4 MC]",
        "3 MC",
    )
    repeat(11) {
      kim.doTask("Pass")
      kim.doTasks("18 MC", "4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    }
    kim.doTask("Pass")
    kim.doTask("18 MC")
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Prelude also shortens the TR63 game to twelve generations`() {
    newTestGame(
        "$options, PreludeExpansion, Tr63SoloObjective, -VenusNextExpansion",
        playerCount = 1,
    )
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "$Donation FROM PreludeCard",
        "21 MC",
    )

    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
        "$AlliedBank FROM PreludeCard",
        "PROD[4 MC]",
        "3 MC",
    )
    repeat(11) {
      kim.doTask("Pass")
      kim.doTasks("18 MC", "4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    }
    kim.doTask("Pass")
    kim.doTask("18 MC")
    admin.count("Generation") shouldBe 12
    kim.count("Victory") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Completing the base parameters wins standard solo without Venus`() {
    newTestGame("$options, -VenusNextExpansion", playerCount = 1)
    prepareFinalSoloTemperature()
    kim.completeOperation {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.doTask("Pass")
    kim.doTask("15 MC")
    kim.count("Victory") shouldBe 1
    kim.doTask("Ok") // No final greenery.
    kim.doTask("15 VictoryPoint")
    kim.count("VictoryPoint") shouldBe 15
  }

  @Test
  internal fun `Completing only the base parameters loses standard solo with Venus`() {
    newTestGame(options, playerCount = 1)
    prepareFinalSoloTemperature()
    kim.completeOperation {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.doTask("Pass")
    kim.doTask("15 MC")
    kim.count("Victory") shouldBe 0
    admin.count("VenusStep") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Completing Venus as well wins standard solo`() {
    newTestGame(options, playerCount = 1)
    prepareFinalSoloTemperature()
    // Leave Venus a step short too; both final increases happen through normal projects.
    agents.correct("14 VenusStep")
    kim.completeOperation {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.completeOperation {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
    }
    kim.doTask("Pass")
    kim.doTask("16 MC")
    kim.count("Victory") shouldBe 1
    kim.doTask("Ok") // No final greenery.
    kim.doTask("16 VictoryPoint")
    kim.count("VictoryPoint") shouldBe 16
  }

  @Test
  internal fun `TR63 wins with incomplete parameters and final greenery scores without TR`() {
    // Jacob corrected his earlier TR63 ruling: https://boardgamegeek.com/article/37132579
    newTestGame("$options, Tr63SoloObjective, -VenusNextExpansion", playerCount = 1)
    // Prepare 62 TR and plants; the last rating increase and final greenery are the subject.
    kim.exMachina("48 TerraformRating, 8 Plant")
    agents.correct("-13 SoloGenerationsLeft")
    kim.completeOperation {
      doTasks(*standardProjectTasks("BufferGasProject"), "16 Pay<> FROM MC", "TerraformRating")
    }
    kim.doTask("Pass")
    kim.doTask("63 MC")
    kim.count("Victory") shouldBe 1
    admin.count("OxygenStep") shouldBe 0
    kim.completeOperation {
          doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Tharsis_3_3>",
          )
        }
        .expect("0 OxygenStep, 0 TerraformRating")
    kim.doTask("Ok") // No further final greenery.
    kim.count("TerraformRating") shouldBe 63
    kim.doTask("64 VictoryPoint")
    kim.count("VictoryPoint") shouldBe 64
  }

  @Test
  internal fun `TR63 loses at sixty two despite abundant money and plants`() {
    newTestGame("$options, Tr63SoloObjective, -VenusNextExpansion", playerCount = 1)
    // Resources cannot substitute for the missing rating at the end of the final generation.
    kim.exMachina("48 TerraformRating, 100 MC, 16 Plant")
    agents.correct("-13 SoloGenerationsLeft")
    kim.doTask("Pass")
    kim.doTask("62 MC")
    kim.count("TerraformRating") shouldBe 62
    kim.count("Victory") shouldBe 0
    admin.count("FinalGreeneryPhase") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Completing Mars below sixty three TR does not win the TR63 objective`() {
    newTestGame("$options, Tr63SoloObjective, -VenusNextExpansion", playerCount = 1)
    prepareFinalSoloTemperature()
    kim.completeOperation {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.doTask("Pass")
    kim.doTask("15 MC")
    kim.count("Victory") shouldBe 0
    admin.count("TemperatureStep") shouldBe 19
    admin.count("OxygenStep") shouldBe 14
    admin.count("OceanTile") shouldBe 9
    kim.count("TerraformRating") shouldBe 15
    admin.count("GameScope") shouldBe 0
  }

  @Test
  internal fun `Losing rating after reaching sixty three still loses the solo game`() {
    newTestGame(
        "$options, Tr63SoloObjective, -VenusNextExpansion, $BoardOfDirectors, $Pristar",
        playerCount = 1,
    )
    // Only the late rating loss matters; install the Board and its unused director directly.
    kim.exMachina("48 TerraformRating, $BoardOfDirectors, Director<$BoardOfDirectors>")
    agents.correct("-13 SoloGenerationsLeft")
    kim.completeOperation {
      doTasks(*standardProjectTasks("BufferGasProject"), "16 Pay<> FROM MC", "TerraformRating")
    }
    kim.count("TerraformRating") shouldBe 63
    kim.completeOperation {
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$Pristar>>",
          "$Pristar FROM CorporationCard<Selecting>",
          "53 MC",
          "-2 TerraformRating",
          "-42 MC",
      )
    }
    kim.doTask("Pass")
    kim.doTask("61 MC")
    kim.count("TerraformRating") shouldBe 61
    kim.count("Victory") shouldBe 0
    admin.count("GameScope") shouldBe 0
  }

  private fun prepareFinalSoloTemperature() {
    // Shared end-condition setup: oxygen and oceans are complete, temperature is a step short.
    // Corrections suppress the incidental terraforming rewards; the final project remains real.
    agents.correct(
        "-13 SoloGenerationsLeft, 18 TemperatureStep, 14 OxygenStep, " +
            "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_3>, OceanTile<Tharsis_1_4>, " +
            "OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_1>, OceanTile<Tharsis_2_6>, " +
            "OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>, OceanTile<Tharsis_4_8>"
    )
  }
}
