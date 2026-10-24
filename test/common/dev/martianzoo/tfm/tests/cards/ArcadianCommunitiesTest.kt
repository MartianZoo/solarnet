package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArcadianCommunitiesTest : TfmSandboxTest() {
  @Test
  internal fun `Initial community requires an empty land area but no adjacency`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.exMachina("NormalCityTile<Tharsis_1_1>")
    kim.exMachina("Community<Tharsis_1_3>")

    kim.inTurn {
      doTask("DoRequiredActions")
      shouldThrow<LimitsException> { doTask("Community<Tharsis_1_1>") }
      shouldThrow<LimitsException> { doTask("Community<Tharsis_1_3>") }
      doTask("Community<Tharsis_9_7>")
    }
  }

  @Test
  internal fun `Initial community can share a Nomads area`() {
    // Jacob Fryxelius: https://boardgamegeek.com/thread/3154812/article/42964868#42964868
    newTestGame(kimCorporation = ArcadianCommunities)
    stan.exMachina("$MarsNomads, NomadsMarker<Tharsis_1_1>")

    kim.inTurn { doTasks("DoRequiredActions", "Community<Tharsis_1_1>") }
        .expect("Community<Tharsis_1_1>, 0 NomadsMarker<Stan>")
    stan.assertCounts(1 to "NomadsMarker<Tharsis_1_1>")
  }

  @Test
  internal fun `Action can place a community on a Nomads area`() {
    // Jacob Fryxelius: https://boardgamegeek.com/thread/3154812/article/42964868#42964868
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn { doTasks("DoRequiredActions", "Community<Tharsis_1_1>") }
    stan.exMachina("$MarsNomads, NomadsMarker<Tharsis_2_1>")

    kim.inTurn {
          doTasks(*cardActionTasks("ArcadianCommunities"), "Community<Tharsis_2_1>")
        }
        .expect("Community<Tharsis_2_1>, 0 NomadsMarker<Stan>")
    stan.assertCounts(1 to "NomadsMarker<Tharsis_2_1>")
  }

  @Test
  internal fun `Action places a community adjacent to an owned tile`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.exMachina("NormalCityTile<Tharsis_1_1>, GreeneryTile<Tharsis_2_2>")
    kim.inTurn { doTasks("DoRequiredActions", "Community<Tharsis_9_7>") }

    kim.inTurn {
      doTasks(*cardActionTasks("$ArcadianCommunities"))
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_2>") }
      shouldThrow<LimitsException> { doTask("Community<Tharsis_2_2>") }
      doTask("Community<Tharsis_2_1>")
    }
  }

  @Test
  internal fun `Action places a community adjacent to an owned community`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("Community<Tharsis_4_2>")
    }

    kim.inTurn {
      doTasks(*cardActionTasks("ArcadianCommunities"), "Community<Tharsis_4_3>")
    }
  }

  @Test
  internal fun `Action does not chain from another player's pieces`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    stan.exMachina("NormalCityTile<Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_4_2>")
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("Community<Tharsis_9_7>")
    }

    kim.inTurn {
      doTasks(*cardActionTasks("ArcadianCommunities"))
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_2_1>") }
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_3>") }
      doTask("Community<Tharsis_9_6>")
    }
  }

  @Test
  internal fun `Developing a community removes it and pays its Arcadian owner`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("Community<Tharsis_1_1>")
    }
    kim.setToExMachina(23, "MC")

    kim.inTurn {
          doTasks(
              *standardProjectTasks("GreeneryProject"),
              "23 Pay<> FROM MC",
              "GreeneryTile<Tharsis_1_1>",
              "3 MC",
          )
          doTask("OxygenStep")
          doTasks("2 Steel", "TerraformRating")
        }
        .expect("-Community, 0 MC<Stan>")
  }

  @Test
  internal fun `Developing a Land Claim community pays its Arcadian owner`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("Community<Tharsis_9_7>")
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("LandClaim"),
          "1 Pay<> FROM MC",
          "LandClaim FROM ProjectCard",
      )
      doTask("Community<Tharsis_1_1>")
    }
    kim.setToExMachina(23, "MC")

    kim.inTurn {
          doTasks(
              *standardProjectTasks("GreeneryProject"),
              "23 Pay<> FROM MC",
              "GreeneryTile<Tharsis_1_1>",
              "3 MC",
          )
          doTask("OxygenStep")
          doTasks("2 Steel", "TerraformRating")
        }
        .expect("-Community")
  }

  @Test
  internal fun `Artificial Lake pays the claim owner even though oceans are unowned`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("Community<Tharsis_1_3>")
    }
    kim.setToExMachina(14, "MC")
    kim.setToExMachina(12, "TemperatureStep")

    shouldThrow<LimitsException> {
      stan.inTurn {
        doTasks(*playCardTasks("ArtificialLake"), "15 Pay<> FROM MC")
        doTask("ArtificialLake FROM ProjectCard")
        doTask("OceanTile<Tharsis_1_3>")
      }
    }
    kim.setToExMachina(15, "MC")
    kim.inTurn {
          doTasks(*playCardTasks("ArtificialLake"))
          doTask("Ok") // Keep the steel.
          doTask("15 Pay<> FROM MC")
          doTasks(
              "ArtificialLake FROM ProjectCard",
              "OceanTile<Tharsis_1_3>",
              "3 MC",
              "TerraformRating",
          )
        }
        .expect("-Community, 0 MC<Stan>")
  }
}
