package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class MarsNomadsTest : TfmSandboxTest() {
  @Test
  internal fun `Places its initial marker without collecting a placement bonus`() {
    newTestGame()

    kim.playProject(MarsNomads, 13) { doTask("NomadsMarker<Tharsis_1_1>") }.expect("0 Steel")
  }

  @Test
  internal fun `Collects the placement bonus when moving to an adjacent area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>") }
        .expect("2 Steel")
  }

  @Test
  internal fun `Cannot move to a nonadjacent area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<NarrowingException> {
      kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_4_2 FROM Tharsis_1_1>") }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "NomadsMarker<Tharsis_4_2>")
  }

  @Test
  internal fun `Cannot move onto an opposing community`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_2_2>")

    shouldThrow<NarrowingException> {
      kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>") }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    stan.assertCounts(1 to "Community<Tharsis_2_2>")
  }

  @Test
  internal fun `Prevents its owner from building a city on the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<DeadEndException> { kim.stdProject("CityProject") { placeTile(1, 1) } }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "CityTile")
  }

  @Test
  internal fun `Prevents an opponent from planting greenery on the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<DeadEndException> { stan.stdProject("GreeneryProject") { placeTile(1, 1) } }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    stan.assertCounts(0 to "GreeneryTile")
  }

  @Test
  internal fun `Allows an opponent to claim the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    stan
        .playProject(LandClaim, 1) { doTask("Community<Tharsis_1_1>") }
        .expect("Community<Stan, Tharsis_1_1>, 0 NomadsMarker<Kim>")
  }

  @Test
  internal fun `Collects the normal bonus for each adjacent ocean when moving`() {
    newTestGame()
    kim.exMachina(
        "$MarsNomads, NomadsMarker<Tharsis_4_7>, OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>"
    )

    kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_5_7 FROM Tharsis_4_7>") }
        .expect("6 MC")
  }

  @Test
  internal fun `Collects the larger ocean bonus with Lakefront Resorts`() {
    newTestGame(kimCorporation = LakefrontResorts)
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_4_6>, OceanTile<Tharsis_4_8>")

    kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_4_7 FROM Tharsis_4_6>") }
        .expect("3 MC")
  }

  @Test
  internal fun `Collects metal without triggering Mining Guild`() {
    newTestGame(kimCorporation = MiningGuild)
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>") }
        .expect("2 Steel, PROD[0 Steel]")
  }

  @Test
  internal fun `Collects its placement bonus without triggering Mars First`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>, Ruling<MarsFirst> FROM Ruling<Greens>")

    kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>") }
        .expect("2 Steel")
  }

  @Test
  internal fun `Does not trigger Philares when moving beside an opponent's tile`() {
    newTestGame(kimCorporation = Philares)
    kim.stdAction("RequiredActionsSignal") { placeTile(2, 2) }
    stan.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    stan
        .cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>") }
        .expect("0 StandardResource<Kim>")
    shouldThrow<TaskException> { kim.doTask("Steel") }
  }

  @Test
  internal fun `Cannot move onto a city`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, NormalCityTile<Kim, Tharsis_2_1>")

    shouldThrow<NarrowingException> {
      kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_2_1 FROM Tharsis_1_1>") }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "NomadsMarker<Tharsis_2_1>")
  }

  @Test
  internal fun `Cannot move onto greenery`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, GreeneryTile<Kim, Tharsis_2_2>")

    shouldThrow<NarrowingException> {
      kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>") }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "NomadsMarker<Tharsis_2_2>")
  }

  @Test
  internal fun `Pays for and builds a colony when moving onto MSL Curiosity`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Io, Europa")
    kim.exMachina("$MarsNomads, NomadsMarker<Cimmeria_3_2>")

    kim.cardAction1(MarsNomads) {
          doTask("NomadsMarker<Cimmeria_3_3 FROM Cimmeria_3_2>")
          doTask("Colony<Luna>")
        }
        .expect("-5 MC, Colony<Luna>, PROD[2 MC], 0 OwnedTile")
  }

  @Test
  internal fun `Cannot move onto MSL Curiosity when all colony tiles already have an own colony`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Io, Europa")
    kim.exMachina(
        "$MarsNomads, NomadsMarker<Cimmeria_3_2>, Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Io>, Colony<Europa>"
    )

    shouldThrow<DependencyException> {
      kim.cardAction1(MarsNomads) { doTask("NomadsMarker<Cimmeria_3_3 FROM Cimmeria_3_2>") }
    }
    kim.assertCounts(
        1 to "NomadsMarker<Cimmeria_3_2>",
        0 to "NomadsMarker<Cimmeria_3_3>",
        42 to "MC",
        5 to "Colony",
    )
  }

  @Ignore // The greenery fallback still treats the marker area as available.
  @Test
  internal fun `Allows greenery elsewhere when its marker blocks the last adjacent area`() {
    plantBeyondNomads().expect("GreeneryTile<Tharsis_9_7>, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `BUG - Blocks greenery elsewhere when its marker blocks the last adjacent area`() {
    shouldThrow<NarrowingException> { plantBeyondNomads() }
    kim.assertCounts(8 to "Plant", 1 to "GreeneryTile", 0 to "GreeneryTile<Tharsis_9_7>")
    stan.count("NomadsMarker<Tharsis_2_2>") shouldBe 1
  }

  private fun plantBeyondNomads(): TaskResult {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_1_1>, 8 Plant")
    stan.exMachina("NormalCityTile<Tharsis_2_1>, $MarsNomads, NomadsMarker<Tharsis_2_2>")
    shouldThrow<DeadEndException> { kim.convertPlants { placeTile(2, 2) } }
    return kim.convertPlants { placeTile(9, 7) }
  }
}
