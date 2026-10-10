package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentRulesTest : TfmSandboxTest() {
  // Resolved FAQ: Advisor may choose a completed parameter to do nothing.
  @Test
  internal fun `Advisor may choose a completed parameter while another is available`() {
    newTestGame(addOptions = "WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 14 VenusStep")
    kim.stdProject("AirScrappingProject")

    kim.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }
        .expect("0 VenusStep, 0 TemperatureStep, 0 TerraformRating")
    kim.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
  }

  @Test
  internal fun `Player Venus increase rolls back when its mandatory colony has no slot`() {
    newTestGame(addOptions = "AmazonisMap, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.exMachina("7 VenusStep")
    fillSelectedColonySlots()

    shouldThrow<LimitsException> {
      kim.stdProject("AirScrappingProject") { doTask("Colony<Luna>") }
    }
    admin.count("VenusStep") shouldBe 7
    kim.count("Colony") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `Advisor Venus increase proceeds with full colony slots`() {
    newTestGame(
        addOptions = "AmazonisMap, WorldGovernmentAdvisor, Luna, Ceres, Triton, Ganymede, Callisto"
    )
    kim.exMachina("$WorldGovernmentAdvisor, 7 VenusStep")
    fillSelectedColonySlots()

    kim.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }
        .expect("VenusStep, 0 Colony<Anyone>, 0 TerraformRating")
  }

  @Test
  internal fun `Advisor Venus increase does not award a player colony`() {
    newTestGame(addOptions = "AmazonisMap, WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 7 VenusStep")

    kim.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }
        .expect("VenusStep, 0 Colony<Anyone>, 0 TerraformRating")
  }

  private fun fillSelectedColonySlots() {
    listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").forEach { track ->
      repeat(3) { stan.exMachina("Colony<$track>") }
    }
  }

  // Resolved FAQ: Advisor remains usable even when every parameter is complete.
  // Earlier discussion: https://boardgamegeek.com/thread/3348438/article/44693194#44693194
  @Test
  internal fun `Advisor action can be spent when every global parameter is complete`() {
    newTestGame(addOptions = "WorldGovernmentAdvisor")
    kim.exMachina("$WorldGovernmentAdvisor, 18 TemperatureStep, 14 VenusStep, 13 OxygenStep")
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_3>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_1>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>"
    )
    kim.setToExMachina(100, "MC")
    kim.stdProject("AsteroidProject")
    kim.stdProject("AirScrappingProject")
    kim.stdProject("GreeneryProject") { placeTile(3, 3) }
    kim.stdProject("AquiferProject") { placeTile(4, 8) }

    kim.cardAction1(WorldGovernmentAdvisor)
        .expect("0 TerraformRating, 0 VenusStep, 0 TemperatureStep, 0 OxygenStep, 0 OceanTile")
    kim.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Solar World Government cannot choose a completed parameter`() {
      newTestGame(playerCount = 2)
      repeat(15) { finishGeneration("VenusStep") }
      stan.pass()
      kim.pass()

      shouldThrow<LimitsException> { stan.wgt("VenusStep") }
      stan.wgt("TemperatureStep").expect("TemperatureStep, 0 TerraformRating")
    }

    @Test
    internal fun `Admin terraforming triggers Aphrodite without granting terraform rating`() {
      newTestGame(playerCount = 2, kimCorporation = Aphrodite)
      kim.pass()
      stan.pass()

      kim.wgt("VenusStep").expect("VenusStep, 2 MC, 0 TerraformRating")
    }

    @Test
    internal fun `Admin terraforming does not trigger Homeostasis Bureau's owner effect`() {
      newTestGame(playerCount = 2)
      kim.turn { playProject(HomeostasisBureau, 16) }
      stan.pass()
      kim.pass()

      kim.wgt("TemperatureStep").expect("TemperatureStep, 0 MC, 0 TerraformRating")
    }

    @Test
    internal fun `Without Venus or World Government passes lead straight to Research`() {
      newTestGame(addOptions = "-VenusNextExpansion", playerCount = 2)
      kim.pass()
      stan.pass()

      kim.buyCards(0)
      stan.buyCards(0)
      admin.count("Generation") shouldBe 2
      admin.count("TemperatureStep") shouldBe 0
    }

    @Test
    internal fun `World Government can be disabled while Venus remains in play`() {
      newTestGame(addOptions = "-WorldGovernmentRule", playerCount = 2)
      kim.pass()
      stan.pass()

      kim.buyCards(0)
      stan.buyCards(0)
      admin.count("Generation") shouldBe 2
      admin.count("VenusStep") shouldBe 0
    }

    @Test
    internal fun `World Government can be selected without Venus`() {
      newTestGame(addOptions = "-VenusNextExpansion, WorldGovernmentRule", playerCount = 2)
      kim.pass()
      stan.pass()

      kim.wgt("TemperatureStep").expect("TemperatureStep, 0 TerraformRating")
    }

    @Test
    internal fun `First player places the standard temperature track's threshold ocean`() {
      newTestGame(playerCount = 2)
      repeat(14) { finishGeneration("TemperatureStep") }
      kim.pass()
      stan.pass()

      kim.wgt("TemperatureStep").expect("TemperatureStep, 0 TerraformRating")
      kim.doTask("OceanTile<Tharsis_1_2> BY Admin").expect("OceanTile, 0 TerraformRating")
    }

    @Test
    internal fun `First player places the extended temperature track's threshold ocean`() {
      newTestGame(addOptions = "AmazonisMap", playerCount = 2)
      repeat(14) { finishGeneration("TemperatureStep") }
      kim.pass()
      stan.pass()

      kim.wgt("TemperatureStep").expect("TemperatureStep, 0 TerraformRating")
      kim.doTask("OceanTile<Amazonis_02_01> BY Admin").expect("OceanTile, 0 TerraformRating")
    }

    @Test
    internal fun `Solo Venus advances a generation with World Government disabled`() {
      newTestGame(addOptions = "-WorldGovernmentRule", playerCount = 1)
      val remaining = admin.count("SoloGenerationsLeft")
      kim.pass()
      kim.buyCards(0)

      admin.count("SoloGenerationsLeft") shouldBe remaining - 1
      admin.count("VenusStep") shouldBe 0
      admin.count("TemperatureStep") shouldBe 0
    }

    private fun finishGeneration(choice: String) {
      val first = players.single { it.count("StartToken") == 1 }
      first.pass()
      players.single { it != first }.pass()
      first.wgt(choice)
      players.forEach { it.buyCards(0) }
    }
  }
}
