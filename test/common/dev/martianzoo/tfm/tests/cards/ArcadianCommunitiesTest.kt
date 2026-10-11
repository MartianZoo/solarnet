package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArcadianCommunitiesTest : TfmSandboxTest() {
  @Test
  internal fun `Initial community requires an empty land area but no adjacency`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.exMachina("NormalCityTile<Tharsis_1_1>")
    kim.exMachina("Community<Tharsis_1_3>")

    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      shouldThrow<LimitsException> { doTask("Community<Tharsis_1_1>") }
      shouldThrow<LimitsException> { doTask("Community<Tharsis_1_3>") }
      doTask("Community<Tharsis_9_7>")
    }
  }

  @Test
  internal fun `Action places a community adjacent to an owned tile`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.exMachina("NormalCityTile<Tharsis_1_1>, GreeneryTile<Tharsis_2_2>")
    kim.inTurn { doTasks("RequiredActionsSignal", "Community<Tharsis_9_7>") }

    kim.inTurn {
      doTask("UseCardAction<$ArcadianCommunities, Action1>")
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_2>") }
      shouldThrow<LimitsException> { doTask("Community<Tharsis_2_2>") }
      doTask("Community<Tharsis_2_1>")
    }
  }

  @Test
  internal fun `Action places a community adjacent to an owned community`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Community<Tharsis_4_2>")
    }

    kim.inTurn {
      doTasks("UseCardAction<ArcadianCommunities, Action1>", "Community<Tharsis_4_3>")
    }
  }

  @Test
  internal fun `Action does not chain from another player's pieces`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    stan.exMachina("NormalCityTile<Tharsis_1_1>")
    stan.exMachina("Community<Tharsis_4_2>")
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Community<Tharsis_9_7>")
    }

    kim.inTurn {
      doTask("UseCardAction<ArcadianCommunities, Action1>")
      selectOnlyTask()
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_2_1>") }
      shouldThrow<NarrowingException> { doTask("Community<Tharsis_4_3>") }
      doTask("Community<Tharsis_9_6>")
    }
  }

  @Test
  internal fun `Developing a community removes it and pays its Arcadian owner`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Community<Tharsis_1_1>")
    }
    kim.setToExMachina(23, "MC")

    kim.inTurn {
          doTasks(
              "UseStandardProject<GreeneryProject>",
              "-23 MC",
              "GreeneryTile<Tharsis_1_1>",
              "3 MC",
          )
          selectTask("OxygenStep.")
          doTasks("2 Steel", "TerraformRating")
        }
        .expect("-Community, 0 MC<Stan>")
  }

  @Test
  internal fun `Developing a Land Claim community pays its Arcadian owner`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Community<Tharsis_9_7>")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<LandClaim>>", "-1 MC", "LandClaim FROM ProjectCard")
      selectOnlyTask()
      doTask("Community<Tharsis_1_1>")
    }
    kim.setToExMachina(23, "MC")

    kim.inTurn {
          doTasks(
              "UseStandardProject<GreeneryProject>",
              "-23 MC",
              "GreeneryTile<Tharsis_1_1>",
              "3 MC",
          )
          selectTask("OxygenStep.")
          doTasks("2 Steel", "TerraformRating")
        }
        .expect("-Community")
  }

  @Test
  internal fun `Artificial Lake pays the claim owner even though oceans are unowned`() {
    newTestGame(kimCorporation = ArcadianCommunities)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Community<Tharsis_1_3>")
    }
    kim.setToExMachina(14, "MC")
    kim.setToExMachina(12, "TemperatureStep")

    shouldThrow<LimitsException> {
      stan.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
        selectTask("-X Steel?")
        selectTask("Ok")
        doTask("ArtificialLake FROM ProjectCard")
        selectOnlyTask()
        doTask("OceanTile<Tharsis_1_3>")
      }
    }
    kim.setToExMachina(15, "MC")
    kim.inTurn {
          doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
          selectTask("-X Steel?")
          doTasks(
              "Ok",
              "ArtificialLake FROM ProjectCard",
              "OceanTile<Tharsis_1_3>",
              "3 MC",
              "TerraformRating",
          )
        }
        .expect("-Community, 0 MC<Stan>")
  }
}
