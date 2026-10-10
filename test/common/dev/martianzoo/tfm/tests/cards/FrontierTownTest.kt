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

    kim.playProject(FrontierTown, 11) {
          placeTile(2, 2)
          repeat(3) { doTask("PartyDelegate<Scientists>") }
        }
        .expect("3 PartyDelegate<Scientists>")
  }

  @Test
  internal fun `An ocean placement reward does not consume the city's remaining repeated bonus`() {
    newTestGame(addOptions = "HellasMap, FrontierTown, TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    kim.playProject(FrontierTown, 11) {
          placeTile(9, 7)
          placeTile(5, 7)
          placeTile(5, 8)
          placeTile(6, 7)
        }
        .expect("CityTile, 3 OceanTile")
  }

  @Test
  internal fun `Triples MSL Curiosity's colony bonus and charges for each colony`() {
    newTestGame(
        addOptions = "CimmeriaMap, FrontierTown, TurmoilExpansion, Luna, Ceres, Triton, Io, Europa"
    )
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    kim.playProject(FrontierTown, 11) {
          placeTile(3, 3)
          doTask("Colony<Luna>")
          doTask("Colony<Ceres>")
          doTask("Colony<Triton>")
        }
        .expect("3 Colony, -26 MC, PROD[2 MC, Steel], 3 Titanium")
  }

  @Test
  internal fun `Cannot choose MSL Curiosity when only two colony tiles are active`() {
    newTestGame(
        addOptions =
            "CimmeriaMap, FrontierTown, TurmoilExpansion, Luna, Ceres, Miranda, Titan, Enceladus"
    )
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    shouldThrow<DependencyException> {
      kim.playProject(FrontierTown, 11) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
        doTask("Colony<Ceres>")
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

    kim.playProject(FrontierTown, 11) { placeTile(4, 5) }.expect("6 Plant, -9 MC, PROD[-Energy]")
  }
}
