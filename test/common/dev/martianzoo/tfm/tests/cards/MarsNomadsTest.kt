package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

// Printed Mars Nomads text: no tiles on its area; move only to adjacent, non-reserved, empty areas.
// Jacob Fryxelius: https://boardgamegeek.com/thread/3154812/article/42964868#42964868
// Jacob Fryxelius on claims blocking movement:
// https://boardgamegeek.com/thread/3154812/article/42966383#42966383
internal class MarsNomadsTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot place its initial marker on the Noctis area`() {
    newTestGame()

    kim.inTurn {
      doTasks(*playCardTasks("MarsNomads"), "13 Pay<> FROM MC")
      doTask("MarsNomads FROM ProjectCard")
      shouldThrow<ExpressionException> { doTask("NomadsMarker<Tharsis_5_3>") }
      doTask("NomadsMarker<Tharsis_1_1>")
    }
    kim.assertCounts(1 to "NomadsMarker", 1 to "NomadsMarker<Tharsis_1_1>")
  }

  @Test
  internal fun `Places its initial marker without collecting a placement bonus`() {
    newTestGame()

    kim.inTurn {
          doTasks(
              *playCardTasks("MarsNomads"),
              "13 Pay<> FROM MC",
              "MarsNomads FROM ProjectCard",
              "NomadsMarker<Tharsis_1_1>",
          )
        }
        .expect("0 Steel")
  }

  @Test
  internal fun `Collects the placement bonus when moving to an adjacent area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    kim.inTurn {
      doTasks(*cardActionTasks("MarsNomads"))
      doTasks("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>", "2 Steel")
    }
  }

  @Test
  internal fun `Cannot move to a nonadjacent area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*cardActionTasks("MarsNomads"))
        doTask("NomadsMarker<Tharsis_4_2 FROM Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "NomadsMarker<Tharsis_4_2>")
  }

  @Test
  internal fun `Cannot move onto an opposing community`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_2_2>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*cardActionTasks("MarsNomads"))
        doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    stan.assertCounts(1 to "Community<Tharsis_2_2>")
  }

  @Test
  internal fun `Cannot move onto its owner's community`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, Community<Tharsis_2_2>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*cardActionTasks("MarsNomads"))
        doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 1 to "Community<Tharsis_2_2>")
  }

  @Test
  internal fun `Prevents its owner from building a city on the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "CityTile<Tharsis_1_1>",
        )
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "CityTile")
  }

  @Test
  internal fun `Prevents an opponent from planting greenery on the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        doTasks(*standardProjectTasks("GreeneryProject"))
        doTask("23 Pay<> FROM MC")
        doTask("GreeneryTile<Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    stan.assertCounts(0 to "GreeneryTile")
  }

  @Test
  internal fun `Prevents a special tile on the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        doTasks(*playCardTasks("MiningRights"), "9 Pay<> FROM MC")
        doTask("MiningRights FROM ProjectCard")
        doTask("MiningRights_SpecialTile<Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    stan.assertCounts(0 to "MiningRights_SpecialTile")
  }

  @Test
  internal fun `Prevents an ocean tile placed on land from entering the marker's area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_3>")
    stan.setToExMachina(12, "TemperatureStep")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        doTasks(*playCardTasks("ArtificialLake"), "15 Pay<> FROM MC")
        doTask("ArtificialLake FROM ProjectCard")
        doTask("OceanTile<Tharsis_1_3>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_3>")
    stan.assertCounts(0 to "OceanTile<Tharsis_1_3>")
  }

  @Test
  internal fun `Prevents greenery on the marker even when its owner claims the area`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, Community<Tharsis_1_1>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
        doTask("GreeneryTile<Tharsis_1_1>")
      }
    }
    kim.assertCounts(
        1 to "NomadsMarker<Tharsis_1_1>",
        1 to "Community<Tharsis_1_1>",
        0 to "GreeneryTile<Tharsis_1_1>",
    )
  }

  @Test
  internal fun `Allows an opponent to claim the marker's area`() {
    // Land Claim permits non-reserved areas; Jacob says the Nomads area is not reserved.
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    stan
        .inTurn {
          doTasks(
              *playCardTasks("LandClaim"),
              "1 Pay<> FROM MC",
              "LandClaim FROM ProjectCard",
          )
          doTask("Community<Tharsis_1_1>")
        }
        .expect("0 NomadsMarker<Kim>")
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
    stan.assertCounts(1 to "Community<Tharsis_1_1>")
  }

  @Test
  internal fun `Allows its owner to claim the marker's area`() {
    // Land Claim permits non-reserved areas; Jacob says the Nomads area is not reserved.
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    kim.inTurn {
          doTasks(*playCardTasks("LandClaim"), "1 Pay<> FROM MC")
          doTasks("LandClaim FROM ProjectCard", "Community<Tharsis_1_1>")
        }
        .expect("0 NomadsMarker<Kim>")
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 1 to "Community<Tharsis_1_1>")
  }

  @Test
  internal fun `Moving away leaves a claim for its owner to build on`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")
    stan.exMachina("Community<Tharsis_2_1>")

    kim.inTurn {
          doTasks(*cardActionTasks("MarsNomads"))
          doTasks("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>", "2 Steel")
        }
        .expect("0 Community<Stan>")
    stan.assertCounts(1 to "Community<Tharsis_2_1>")

    stan
        .inTurn {
          doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
          doTask("GreeneryTile<Tharsis_2_1>")
          doTasks("OxygenStep", "TerraformRating")
        }
        .expect("-Community, GreeneryTile<Tharsis_2_1>")
  }

  @Test
  internal fun `Collects the normal bonus for each adjacent ocean when moving`() {
    newTestGame()
    kim.exMachina(
        "$MarsNomads, NomadsMarker<Tharsis_4_7>, OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>"
    )

    kim.inTurn {
      doTasks(*cardActionTasks("MarsNomads"))
      doTasks("NomadsMarker<Tharsis_5_7 FROM Tharsis_4_7>", "2 Plant", "2 MC", "2 MC", "2 MC")
    }
  }

  @Test
  internal fun `Collects the larger ocean bonus with Lakefront Resorts`() {
    newTestGame(addOptions = "LakefrontResorts", kimCorporation = LakefrontResorts)
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_4_6>, OceanTile<Tharsis_4_8>")

    kim.inTurn {
          doTasks(*cardActionTasks("MarsNomads"))
          doTasks("NomadsMarker<Tharsis_4_7 FROM Tharsis_4_6>", "Plant")
          doTask("MC")
          doTask("2 MC")
        }
        .expect("3 MC")
  }

  @Test
  internal fun `Collects metal without triggering Mining Guild`() {
    newTestGame(kimCorporation = MiningGuild)
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    kim.inTurn {
          doTasks(*cardActionTasks("MarsNomads"))
          doTasks("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>", "2 Steel")
        }
        .expect("PROD[0 Steel]")
  }

  @Test
  internal fun `Collects its placement bonus without triggering Mars First`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>, Ruling<MarsFirst FROM Greens>")

    kim.inTurn {
      doTasks(*cardActionTasks("MarsNomads"))
      doTasks("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>", "2 Steel")
    }
  }

  @Test
  internal fun `Does not trigger Philares when moving beside an opponent's tile`() {
    newTestGame(kimCorporation = Philares)
    kim.inTurn {
      doTasks("DoRequiredActions", "GreeneryTile<Tharsis_2_2>")
      doTask("OxygenStep")
      doTasks("Steel", "TerraformRating")
    }
    stan.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    stan
        .inTurn {
          doTasks(*cardActionTasks("MarsNomads"))
          doTasks("NomadsMarker<Tharsis_1_1 FROM Tharsis_2_1>", "2 Steel")
        }
        .expect("0 StandardResource<Kim>")
    shouldThrow<TaskException> { kim.doTask("Steel") }
  }

  @Test
  internal fun `Cannot move onto a city`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, NormalCityTile<Kim, Tharsis_2_1>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*cardActionTasks("MarsNomads"))
        doTask("NomadsMarker<Tharsis_2_1 FROM Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "NomadsMarker<Tharsis_2_1>")
  }

  @Test
  internal fun `Cannot move onto greenery`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, GreeneryTile<Kim, Tharsis_2_2>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*cardActionTasks("MarsNomads"))
        doTask("NomadsMarker<Tharsis_2_2 FROM Tharsis_1_1>")
      }
    }
    kim.assertCounts(1 to "NomadsMarker<Tharsis_1_1>", 0 to "NomadsMarker<Tharsis_2_2>")
  }

  @Test
  internal fun `Pays for and builds a colony when moving onto MSL Curiosity`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Io, Europa")
    kim.exMachina("$MarsNomads, NomadsMarker<Cimmeria_3_2>")

    kim.inTurn {
          doTasks(*cardActionTasks("MarsNomads"))
          doTask("NomadsMarker<Cimmeria_3_3 FROM Cimmeria_3_2>")
          doTask("CimmeriaPlacementBonus!")
          doTasks("-5 MC", "Colony<Luna>", "PROD[2 MC]")
        }
        .expect("0 OwnedTile")
  }

  @Test
  internal fun `Cannot move onto MSL Curiosity when all colony tiles already have an own colony`() {
    newTestGame(addOptions = "CimmeriaMap, Luna, Ceres, Triton, Io, Europa")
    kim.exMachina(
        "$MarsNomads, NomadsMarker<Cimmeria_3_2>, Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Io>, Colony<Europa>"
    )

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(*cardActionTasks("MarsNomads"))
        doTask("NomadsMarker<Cimmeria_3_3 FROM Cimmeria_3_2>")
        doTask("CimmeriaPlacementBonus!")
        doTask("-5 MC")
        // Resolve the colony offer to prove that no eligible colony tile remains.
        selectTask("Colony<>")
      }
    }
    kim.assertCounts(
        1 to "NomadsMarker<Cimmeria_3_2>",
        0 to "NomadsMarker<Cimmeria_3_3>",
        42 to "MC",
        5 to "Colony",
    )
  }

  @Test
  internal fun `Allows greenery elsewhere when its marker blocks the last adjacent area`() {
    // Official rule: https://fryxgames.se/wp-content/uploads/2023/04/TMRULESFINAL.pdf
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_1_1>, 8 Plant")
    stan.exMachina("NormalCityTile<Tharsis_2_1>, $MarsNomads, NomadsMarker<Tharsis_2_2>")
    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTask("UseAction<ConvertPlantsAction, Action1>")
        doTask("8 Pay<Class<Plant>> FROM Plant")
        doTask("GreeneryTile<Tharsis_2_2>")
      }
    }
    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_9_7>")
          doTasks("OxygenStep", "TerraformRating")
        }
        .expect("GreeneryTile<Tharsis_9_7>, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Allows greenery elsewhere when its own marker blocks the last adjacent area`() {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_1_1>, $MarsNomads, NomadsMarker<Tharsis_2_2>, 8 Plant")
    stan.exMachina("NormalCityTile<Tharsis_2_1>")

    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_9_7>")
          doTasks("OxygenStep", "TerraformRating")
        }
        .expect("GreeneryTile<Tharsis_9_7>, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Nomads does not open distant greenery while another adjacent area is available`() {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_1_1>, 8 Plant")
    stan.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_2>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
        doTask("GreeneryTile<Tharsis_9_7>")
      }
    }
    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_2_1>")
          doTasks("OxygenStep", "TerraformRating")
        }
        .expect("GreeneryTile<Tharsis_2_1>, OxygenStep, TerraformRating")
  }

  @Test
  internal fun `Allows greenery elsewhere when a Nomads marker shares the last adjacent area with an own claim`() {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_1_1>, Community<Tharsis_2_2>, 8 Plant")
    stan.exMachina("NormalCityTile<Tharsis_2_1>, $MarsNomads, NomadsMarker<Tharsis_2_2>")

    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_9_7>")
          doTasks("OxygenStep", "TerraformRating")
        }
        .expect("GreeneryTile<Tharsis_9_7>, OxygenStep, TerraformRating")
    kim.assertCounts(1 to "Community<Tharsis_2_2>")
    stan.assertCounts(1 to "NomadsMarker<Tharsis_2_2>")
  }

  @Test
  internal fun `Its marker does not count as an owned tile for greenery adjacency`() {
    newTestGame()
    kim.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>, 8 Plant")

    kim.inTurn {
          doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
          doTask("GreeneryTile<Tharsis_9_7>")
          doTasks("OxygenStep", "TerraformRating")
        }
        .expect("GreeneryTile<Tharsis_9_7>, OxygenStep, TerraformRating")
  }
}
