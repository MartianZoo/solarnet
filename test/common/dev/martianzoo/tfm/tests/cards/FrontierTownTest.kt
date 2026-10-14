package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.FrontierTown
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FrontierTownTest : TfmSandboxTest() {
  @Test
  internal fun `Triples a delegate placement bonus when Turmoil is present`() {
    newTestGame(addOptions = "AmazonisMap, FrontierTown, TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    kim.inTurn {
      doTasks("PlayProject<Class<FrontierTown>>", "-11 MC")
      doTasks("Ok" /* No Steel payment. */, "FrontierTown FROM ProjectCard")
      doTask("FrontierTownBonus!")
      doTasks("PROD[-Energy]", "CityTile<Amazonis_02_02>")
      doTask("-FrontierTownBonus!")
      // Hand the repeat-bonus signal to Admin so the area offers its bonus again.
      selectTask("RepeatPlacementBonus<Kim, Amazonis_02_02>!")
      doTasks("PartyDelegate<Scientists>", "PartyDelegate<Scientists>")
      doTask("PartyDelegate<Scientists>")
    }
  }

  @Test
  internal fun `An ocean placement reward does not consume the city's remaining repeated bonus`() {
    newTestGame(addOptions = "HellasMap, FrontierTown, TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    kim.inTurn {
      doTasks("PlayProject<Class<FrontierTown>>", "-11 MC")
      doTasks("Ok" /* No Steel payment. */, "FrontierTown FROM ProjectCard")
      doTask("FrontierTownBonus!")
      doTasks("PROD[-Energy]", "CityTile<Hellas_9_7>")
      doTask("-FrontierTownBonus!")
      doTask("-6 MC")
      // Hand the repeat-bonus signal to Admin so the area offers its bonus again.
      selectTask("RepeatPlacementBonus<Kim, Hellas_9_7>!")
      doTasks(
          "-6 MC",
          "-6 MC",
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
        addOptions = "CimmeriaMap, FrontierTown, TurmoilExpansion, Luna, Ceres, Triton, Io, Europa"
    )
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    kim.inTurn {
      doTasks("PlayProject<Class<FrontierTown>>", "-11 MC")
      doTasks("Ok" /* No Steel payment. */, "FrontierTown FROM ProjectCard")
      doTask("FrontierTownBonus!")
      doTasks("PROD[-Energy]", "CityTile<Cimmeria_3_3>")
      doTask("-FrontierTownBonus!")
      doTask("CimmeriaPlacementBonus!")
      // Hand the repeat-bonus signal to Admin so the area offers its bonus again.
      selectTask("RepeatPlacementBonus<Kim, Cimmeria_3_3>!")
      doTask("-5 MC")
      doTask("CimmeriaPlacementBonus!")
      doTask("CimmeriaPlacementBonus!")
      doTasks(
          "-5 MC",
          "-5 MC",
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
            "CimmeriaMap, FrontierTown, TurmoilExpansion, Luna, Ceres, Miranda, Titan, Enceladus"
    )
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("PlayProject<Class<FrontierTown>>", "-11 MC")
        doTask("Ok" /* No Steel payment. */)
        doTask("FrontierTown FROM ProjectCard")
        doTask("FrontierTownBonus!")
        doTask("PROD[-Energy]")
        doTask("CityTile<Cimmeria_3_3>")
        doTask("-FrontierTownBonus!")
        doTask("CimmeriaPlacementBonus!")
        // Hand the repeat-bonus signal to Admin so the area offers its bonus again.
        selectTask("RepeatPlacementBonus<Kim, Cimmeria_3_3>!")
        doTask("-5 MC")
        doTask("CimmeriaPlacementBonus!")
        doTask("CimmeriaPlacementBonus!")
        doTask("-5 MC")
        doTask("-5 MC")
        doTask("Colony<Luna>")
        doTask("PROD[2 MC]")
        doTask("Colony<Ceres>")
        doTask("PROD[Steel]")
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
    newTestGame("FrontierTown, TurmoilExpansion")
    kim.exMachina("OceanTile<Tharsis_5_5>, 2 PartyDelegate<MarsFirst>")

    kim.inTurn {
      doTasks("PlayProject<Class<FrontierTown>>", "-11 MC")
      doTasks("Ok" /* No Steel payment. */, "FrontierTown FROM ProjectCard")
      doTask("FrontierTownBonus!")
      doTasks("PROD[-Energy]", "CityTile<Tharsis_4_5>")
      doTask("-FrontierTownBonus!")
      doTasks("2 Plant", "2 MC")
      doTask("RepeatPlacementBonus<Tharsis_4_5>")
      doTasks("2 Plant", "2 Plant")
    }
  }
}
