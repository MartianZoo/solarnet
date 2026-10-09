package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Aphrodite
import dev.martianzoo.tfm.tests.cards.cardnames.HomeostasisBureau
import dev.martianzoo.tfm.tests.cards.cardnames.WorldGovernmentAdvisor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentRulesTest : CardTest() {
  // Resolved FAQ: Advisor may choose a completed parameter to do nothing.
  @Test
  internal fun `Advisor may choose a completed parameter while another is available`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.runOperation("15 VenusStep")
    admin.phase("Action")

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }
        .expect("0 VenusStep, 0 TemperatureStep, 0 TerraformRating")
    p1.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
  }

  @Test
  internal fun `Solar phase World Government cannot choose a completed parameter`() {
    newGame(VenusNextExpansion)
    p1.runOperation("15 VenusStep")
    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }

    shouldThrow<LimitsException> { p1.doTask("VenusStep! BY Admin") }
    p1.doTask("TemperatureStep! BY Admin")
  }

  @Test
  internal fun `Admin terraforming triggers Aphrodite without granting terraform rating`() {
    newGame(VenusNextExpansion, PromoCardPack)
    p1.runOperation("$Aphrodite")
    val moneyBefore = p1.count("MC")
    val ratingBefore = p1.count("TerraformRating")
    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }

    p1.doTask("VenusStep! BY Admin")

    p1.count("MC") shouldBe moneyBefore + 2
    p1.count("TerraformRating") shouldBe ratingBefore
  }

  @Test
  internal fun `Admin terraforming does not trigger an owner-only effect`() {
    newGame(VenusNextExpansion, PromoCardPack)
    p1.runOperation("$HomeostasisBureau")
    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }

    p1.doTask("TemperatureStep! BY Admin")

    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `World Government is absent when unselected or disabled in Venus`() {
    newGame()
    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }
    game.isIdle() shouldBe true

    newGame(
        GameConfig(
            "VenusNextExpansion, -WorldGovernmentRule",
            "Player1",
            "Player2",
        )
    )
    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }

    game.isIdle() shouldBe true
  }

  @Test
  internal fun `World Government can be selected without Venus`() {
    newGame(GameConfig("WorldGovernmentRule", "Player1", "Player2"))

    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }
    p1.doTask("TemperatureStep! BY Admin")

    p1.count("TemperatureStep") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `first player places a standard-track threshold ocean for World Government`() {
    newGame(GameConfig("WorldGovernmentRule", "Player1", "Player2"))
    admin.runOperation("14 TemperatureStep")

    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("OceanTile<Tharsis_1_2> BY Admin")

    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `first player places an extended-track threshold ocean for World Government`() {
    newGame(GameConfig("AmazonisMap, WorldGovernmentRule", "Player1", "Player2"))
    admin.runOperation("14 TemperatureStep")

    with(agents[ADMIN]) {
      beginOperation("SolarPhase FROM Phase")
      beginOperation("VenusSolarPhase FROM Phase")
    }
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("OceanTile<Amazonis_02_01> BY Admin")

    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Amazonis_02_01>") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `Solo Venus advances a generation with World Government disabled`() {
    newGame(GameConfig("VenusNextExpansion, -WorldGovernmentRule", "Me"))
    val generationsBefore = admin.count("SoloGenerationsLeft")
    admin.phase("Action")

    admin.nextGeneration(0)

    admin.count("SoloGenerationsLeft") shouldBe generationsBefore - 1
    admin.count("VenusStep") shouldBe 0
    admin.count("TemperatureStep") shouldBe 0
  }

  @Test
  internal fun `Player Venus increase rolls back when its mandatory colony has no slot`() {
    newGame(Amazonis, VenusNextExpansion, ColoniesExpansion, colonyTiles = testColonyTiles(2))
    admin.runOperation("7 VenusStep")
    fillSelectedColonySlots()

    shouldThrow<LimitsException> { p1.runOperation("VenusStep") { doTask("Colony<Luna>") } }
    admin.count("VenusStep") shouldBe 7
    p1.count("Colony") shouldBe 0
  }

  @Test
  internal fun `World Government Venus increase proceeds with full colony slots`() {
    newGame(
        Amazonis,
        VenusNextExpansion,
        ColoniesExpansion,
        PreludeExpansion,
        Prelude2CardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.runOperation("7 VenusStep")
    fillSelectedColonySlots()
    admin.phase("Action")

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }
    admin.count("VenusStep") shouldBe 8
    p1.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
    p1.count("Colony") shouldBe 0
  }

  @Test
  internal fun `World Government Venus increase does not award a player colony`() {
    newGame(
        Amazonis,
        VenusNextExpansion,
        ColoniesExpansion,
        PreludeExpansion,
        Prelude2CardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.runOperation("7 VenusStep")
    admin.phase("Action")

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep BY Admin") }

    admin.count("VenusStep") shouldBe 8
    p1.count("Colony") shouldBe 0
    requireP2().count("Colony") shouldBe 0
  }

  private fun fillSelectedColonySlots() {
    val p2 = requireP2()
    listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").forEach { track ->
      repeat(3) { p2.runOperation("Colony<$track>") }
    }
  }

  // Resolved FAQ: Advisor remains usable even when every parameter is complete.
  // Earlier discussion: https://boardgamegeek.com/thread/3348438/article/44693194#44693194
  @Test
  internal fun `Advisor action can be spent when every global parameter is complete`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.runOperation("19 TemperatureStep") {
      p1.doTask("OceanTile<Tharsis_1_2> BY Admin")
    }
    admin.runOperation("15 VenusStep, 14 OxygenStep")
    listOf("1_3", "1_4", "1_5", "2_1", "2_6", "3_2", "4_3", "4_8").forEach {
      admin.runOperation("OceanTile<Tharsis_$it>")
    }
    admin.phase("Action")
    val trBefore = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor)
    p1.count("ActionUsedMarker<$WorldGovernmentAdvisor>") shouldBe 1
    p1.count("TerraformRating") shouldBe trBefore
    admin.count("VenusStep") shouldBe 15
    admin.count("TemperatureStep") shouldBe 19
    admin.count("OxygenStep") shouldBe 14
    admin.count("OceanTile") shouldBe 9
  }
}
