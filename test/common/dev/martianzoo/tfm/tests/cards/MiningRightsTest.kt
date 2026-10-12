package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Ignore
import kotlin.test.Test

internal class MiningRightsTest : TfmSandboxTest() {
  // Resolved FAQ: a wild-resource area is eligible even when its chosen resource is not metal.
  // https://boardgamegeek.com/thread/3403085/article/45161764#45161764
  @Ignore // Known defect: the production requirement rejects wild-resource areas.
  @Test
  internal fun `Can use a wild placement bonus`() {
    playOnWildBonus().expect("Steel, PROD[Steel], MiningRights_SpecialTile")
  }

  @Test
  internal fun `BUG - Cannot use a wild placement bonus`() {
    shouldThrow<RequirementException> { playOnWildBonus() }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 0 to "MiningRights_SpecialTile")
  }

  private fun playOnWildBonus(): TaskResult {
    newTestGame(addOptions = "AmazonisMap, Unsafe")
    return kim.inTurn {
      doTasks("PlayProject<Class<MiningRights>>", "-9 MC")
      selectTask("-X? Steel")
      doTasks("Ok", "MiningRights FROM ProjectCard", "MiningRights_SpecialTile<Amazonis_05_03>")
      selectTask("StandardResource!")
      doTask("Steel")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    // Resolved FAQ: copying allows any originally available metal, regardless of the first choice.
    // https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce
    newTestGame(addOptions = "CimmeriaMap")

    kim.inTurn {
      doTasks("PlayProject<Class<MiningRights>>", "-9 MC")
      selectTask("-X? Steel")
      doTasks(
          "Ok",
          "MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Cimmeria_6_4>",
          "Titanium",
          "2 Steel",
          "PROD[Steel]",
      )
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<RoboticWorkforce>>",
          "-9 MC",
          "RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<MiningRights>",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cannot select an area without a metal placement bonus`() {
    newTestGame()
    shouldThrow<GameplayException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<MiningRights>>", "-9 MC")
        selectTask("-X? Steel")
        selectTask("Ok")
        doTask("MiningRights FROM ProjectCard")
        selectOnlyTask()
        doTask("MiningRights_SpecialTile<Tharsis_2_1>")
        selectOnlyTask()
      }
    }
  }

  @Test
  internal fun `A mixed metal placement grants both resources and steel production`() {
    newTestGame(addOptions = "CimmeriaMap")

    kim.inTurn {
      doTasks("PlayProject<Class<MiningRights>>", "-9 MC")
      selectTask("-X? Steel")
      doTasks(
          "Ok",
          "MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Cimmeria_6_4>",
          "Titanium",
          "2 Steel",
          "PROD[Steel]",
      )
    }
  }

  @Test
  internal fun `A mixed metal placement grants both resources and titanium production`() {
    newTestGame(addOptions = "CimmeriaMap")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<$MiningRights>>",
          "-9 MC",
          "Ok", // No steel paid.
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Cimmeria_6_4>",
          "2 Steel",
          "Titanium",
          "PROD[Titanium]",
      )
    }
  }
}
