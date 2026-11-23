package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FrontierTownTest : TfmSandboxTest() {
  @Test
  internal fun `Triples a delegate placement bonus when Turmoil is present`() {
    newTestGame(addOptions = "AmazonisMap, $FrontierTown, TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    kim.inTurn {
      doTasks(
          *playCardTasks("$FrontierTown"),
          "11 Pay<> FROM MC",
          "FrontierTownBonus!",
          "PROD[-Energy]",
          "CityTile<Amazonis_02_02>",
          "-FrontierTownBonus!",
      )
      // Execute the repeat-bonus signal so the area offers its bonus again.
      doTasks(
          "RepeatPlacementBonus<Amazonis_02_02>!",
          "PartyLeader<Scientists>",
          "PartyFollower<Scientists>",
          "PartyFollower<Scientists>",
      )
    }
  }

  @Test
  internal fun `An ocean placement reward does not consume the city's remaining repeated bonus`() {
    newTestGame(addOptions = "HellasMap, $FrontierTown, TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    kim.inTurn {
      doTasks(
          *playCardTasks("$FrontierTown"),
          "11 Pay<> FROM MC",
          "FrontierTownBonus!",
          "PROD[-Energy]",
          "CityTile<Hellas_9_7>",
          "-FrontierTownBonus!",
          "-6 MC",
      )
      // Execute the repeat-bonus signal so the area offers its bonus again.
      doTasks(
          "RepeatPlacementBonus<Hellas_9_7>!",
          "-12 MC",
          "OceanTile<Hellas_5_7>",
          "TerraformRating",
          "3 Heat",
          "OceanTile<Hellas_5_8>",
          "TerraformRating",
          "2 MC",
          "OceanTile<Hellas_6_7>",
          "TerraformRating",
          "2 MC",
      )
    }
  }

  @Test
  internal fun `Triples MSL Curiosity's colony bonus and charges for each colony`() {
    newTestGame(
        addOptions = "CimmeriaMap, $FrontierTown, TurmoilExpansion, Luna, Ceres, Triton, Io, Europa"
    )
    kim.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    kim.inTurn {
      doTasks(
          *playCardTasks("$FrontierTown"),
          "11 Pay<> FROM MC",
          "FrontierTownBonus!",
          "PROD[-Energy]",
          "CityTile<Cimmeria_3_3>",
          "-FrontierTownBonus!",
          "CimmeriaPlacementBonus!",
      )
      // Execute the repeat-bonus signal so the area offers its bonus again.
      doTasks(
          "RepeatPlacementBonus<Cimmeria_3_3>!",
          "-5 MC",
          "CimmeriaPlacementBonus!",
          "CimmeriaPlacementBonus!",
          "-10 MC",
          "Colony<Luna>",
          "PROD[2 MC]",
          "Colony<Ceres>",
          "PROD[Steel]",
          "Colony<Triton>",
          "3 Titanium",
      )
    }
  }

  @Test
  internal fun `Cannot choose MSL Curiosity when only two colony tiles are active`() {
    newTestGame(
        addOptions =
            "CimmeriaMap, $FrontierTown, TurmoilExpansion, Luna, Ceres, Miranda, Titan, Enceladus"
    )
    kim.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("$FrontierTown"),
            "11 Pay<> FROM MC",
            "FrontierTownBonus!",
            "PROD[-Energy]",
            "CityTile<Cimmeria_3_3>",
            "-FrontierTownBonus!",
            "CimmeriaPlacementBonus!",
        )
        // Execute the repeat-bonus signal so the area offers its bonus again.
        doTasks(
            "RepeatPlacementBonus<Cimmeria_3_3>!",
            "-5 MC",
            "CimmeriaPlacementBonus!",
            "CimmeriaPlacementBonus!",
            "-5 MC",
            "-5 MC",
            "Colony<Luna>",
            "PROD[2 MC]",
            "Colony<Ceres>",
            "PROD[Steel]",
        )
        // Resolve the remaining colony offer to prove that no eligible tile remains.
        selectTask("Colony<>")
      }
    }
    kim.assertCounts(
        0 to "$FrontierTown",
        0 to "CityTile<Cimmeria_3_3>",
        0 to "Colony",
        10 to "ProjectCard",
        1 to "PROD[Energy]",
        42 to "MC",
    )
  }

  @Test
  internal fun `Repeats the printed bonus without repeating the ocean adjacency payout`() {
    newTestGame("$FrontierTown, TurmoilExpansion")
    kim.exMachina("OceanTile<Tharsis_5_5>, PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    kim.inTurn {
      doTasks(
          *playCardTasks("$FrontierTown"),
          "11 Pay<> FROM MC",
          "FrontierTownBonus!",
          "PROD[-Energy]",
          "CityTile<Tharsis_4_5>",
          "-FrontierTownBonus!",
          "2 Plant",
          "2 MC",
          "RepeatPlacementBonus<Tharsis_4_5>",
          "4 Plant",
      )
    }
  }
}
