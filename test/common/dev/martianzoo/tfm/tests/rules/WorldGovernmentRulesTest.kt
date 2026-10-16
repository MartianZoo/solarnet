package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentRulesTest : TfmSandboxTest() {
  // Unresolved adjudication (question 22, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3775830/article/48242149#48242149
  @Test
  internal fun `Advisor may choose a completed parameter while another is available`() {
    newTestGame(addOptions = "WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 14 VenusStep")
    kim.inTurn {
      doTasks("UseStandardProject<AirScrappingProject>", "15 Pay<> FROM MC")
      doTask("VenusStep")
      doTask("TerraformRating")
    }

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("VenusStep BY Admin")
        }
        .expect("0 VenusStep, 0 TemperatureStep, 0 TerraformRating")
    kim.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
  }

  // Unresolved adjudication (question 23, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3407295/article/45199222#45199222
  @Test
  internal fun `Player Venus increase rolls back when its mandatory colony has no slot`() {
    newTestGame(addOptions = "AmazonisMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.exMachina("7 VenusStep")
    fillSelectedColonySlots()

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseStandardProject<AirScrappingProject>")
        doTask("15 Pay<> FROM MC")
        doTask("VenusStep")
        doTask("TerraformRating")

        doTask("Colony<Luna>")
      }
    }
    admin.count("VenusStep") shouldBe 7
    kim.count("Colony") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  // Unresolved adjudication (question 24, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3407295
  @Test
  internal fun `Advisor Venus increase proceeds with full colony slots`() {
    newTestGame(
        addOptions = "AmazonisMap, WorldGovernmentAdvisor, Luna, Ceres, Triton, Ganymede, Callisto"
    )
    kim.exMachina("$WorldGovernmentAdvisor, 7 VenusStep")
    fillSelectedColonySlots()

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("VenusStep BY Admin")
        }
        .expect("0 Colony<Anyone>, 0 TerraformRating")
  }

  // Unresolved adjudication (question 24, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3407295
  @Test
  internal fun `Advisor Venus increase does not award a player colony`() {
    newTestGame(addOptions = "AmazonisMap, WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 7 VenusStep")

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")

          doTask("VenusStep BY Admin")
        }
        .expect("0 Colony<Anyone>, 0 TerraformRating")
  }

  private fun fillSelectedColonySlots() {
    fillColonySlots("Luna")
    fillColonySlots("Ceres")
    fillColonySlots("Triton")
    fillColonySlots("Ganymede")
    fillColonySlots("Callisto")
  }

  private fun fillColonySlots(track: String) {
    // Keep separate placements: a batched gain of three colonies overshoots the track.
    stan.setToExMachina(1, "Colony<$track>")
    stan.setToExMachina(2, "Colony<$track>")
    stan.setToExMachina(3, "Colony<$track>")
  }

  // Earlier discussion: https://boardgamegeek.com/thread/3348438/article/44693194#44693194
  // Unresolved adjudication (question 22, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3775830/article/48242149#48242149
  @Test
  internal fun `Advisor action can be spent when every global parameter is complete`() {
    newTestGame(addOptions = "WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 18 TemperatureStep, 14 VenusStep, 13 OxygenStep")
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_3>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_1>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>"
    )
    kim.setToExMachina(100, "MC")
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks("UseStandardProject<AirScrappingProject>", "15 Pay<> FROM MC")
      doTask("VenusStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_3_3>",
      )
      doTask("OxygenStep")
      doTasks("2 MC", "2 MC", "TerraformRating")
    }
    kim.inTurn {
      doTasks(
          "UseStandardProject<AquiferProject>",
          "18 Pay<> FROM MC",
          "OceanTile<Tharsis_4_8>",
          "TerraformRating",
          "2 Plant",
      )
    }

    kim.inTurn {
          doTask("UseCardAction<WorldGovernmentAdvisor, Action1>")
          // Resolve the world-government handoff even though all tracks are complete.
          selectTask("GlobalParameter* BY Admin")
        }
        .expect("0 TerraformRating, 0 VenusStep, 0 TemperatureStep, 0 OxygenStep, 0 OceanTile")
    kim.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Solar World Government cannot choose a completed parameter`() {
      newTestGame(playerCount = 2)
      repeat(15) { finishGeneration("VenusStep") }
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      shouldThrow<LimitsException> { stan.doTask("VenusStep! BY Admin") }
      stan.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 TerraformRating")
    }

    @Test
    internal fun `Admin terraforming does not trigger Homeostasis Bureau's owner effect`() {
      newTestGame(playerCount = 2)
      kim.doTasks("PlayProject<Class<$HomeostasisBureau>>")
      kim.doTask("0 StandardAction") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 MC, 0 TerraformRating")
    }

    @Test
    internal fun `Without Venus or World Government passes lead straight to Research`() {
      newTestGame(addOptions = "-VenusNextExpansion", playerCount = 2)
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")
      admin.count("Generation") shouldBe 2
      admin.count("TemperatureStep") shouldBe 0
    }

    @Test
    internal fun `World Government can be disabled while Venus remains in play`() {
      newTestGame(addOptions = "-WorldGovernmentRule", playerCount = 2)
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")
      admin.count("Generation") shouldBe 2
      admin.count("VenusStep") shouldBe 0
    }

    @Test
    internal fun `World Government can be selected without Venus`() {
      newTestGame(addOptions = "-VenusNextExpansion, WorldGovernmentRule", playerCount = 2)
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 TerraformRating")
    }

    @Test
    internal fun `First player places the standard temperature track's threshold ocean`() {
      newTestGame(playerCount = 2)
      repeat(14) { finishGeneration("TemperatureStep") }
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 TerraformRating")

      kim.doTask("OceanTile<Tharsis_1_2> BY Admin").expect("OceanTile, 0 TerraformRating")
    }

    @Test
    internal fun `First player places the extended temperature track's threshold ocean`() {
      newTestGame(addOptions = "AmazonisMap", playerCount = 2)
      repeat(14) { finishGeneration("TemperatureStep") }
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("TemperatureStep! BY Admin").expect("TemperatureStep, 0 TerraformRating")

      kim.doTask("OceanTile<Amazonis_02_01> BY Admin").expect("OceanTile, 0 TerraformRating")
    }

    @Test
    internal fun `Solo Venus advances a generation with World Government disabled`() {
      newTestGame(addOptions = "-WorldGovernmentRule", playerCount = 1)
      expectChanges("-SoloGenerationsLeft") {
        kim.doTask("Pass")
        players.forEach { it.autoExecNow(EAGER) }
        kim.doTask("-4 ProjectCard<Selecting>")
      }
      admin.count("VenusStep") shouldBe 0
      admin.count("TemperatureStep") shouldBe 0
    }

    // Unresolved adjudication (question 24): the government currently ignores the colony bonus.
    // https://boardgamegeek.com/thread/3407295
    @Test
    internal fun `Solar World Government currently skips Amazonis Venus colony bonus`() {
      newTestGame(addOptions = "AmazonisMap", playerCount = 2)
      repeat(7) { finishGeneration("VenusStep") }
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      stan.doTask("VenusStep! BY Admin").expect("VenusStep, 0 Colony<Anyone>, 0 TerraformRating")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    }

    private fun finishGeneration(choice: String) {
      val first = players.single { it.count("StartToken") == 1 }
      first.doTask("Pass")
      players.single { it != first }.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      first.doTask("$choice! BY Admin")

      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    }
  }
}
