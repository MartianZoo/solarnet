package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
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
    shouldThrow<DependencyException> { playOnWildBonus() }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 0 to "MiningRights_SpecialTile")
  }

  private fun playOnWildBonus(): TaskResult {
    newTestGame(addOptions = "AmazonisMap, Unsafe")
    return kim.inTurn {
      doTasks("PlayProject<Class<MiningRights>>", "9 Pay<> FROM MC")
      doTasks(
          "MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Amazonis_05_03>",
      )

      doTask("Steel")
      // Resolve the production choice to expose the known wild-area requirement failure.
      selectOnlyTask()
    }
  }

  // Unresolved adjudication (question 21, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/2663453
  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    newTestGame(addOptions = "CimmeriaMap")

    kim.inTurn {
      doTasks("PlayProject<Class<MiningRights>>", "9 Pay<> FROM MC")
      doTasks(
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
          "9 Pay<> FROM MC",
          "RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<MiningRights>",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cannot select an area without a metal placement bonus`() {
    newTestGame()
    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("PlayProject<Class<MiningRights>>", "9 Pay<> FROM MC")
        doTask("MiningRights FROM ProjectCard")
        doTask("MiningRights_SpecialTile<Tharsis_2_1>")
        // Resolve the production choice to check that this area supplies a metal bonus.
        selectOnlyTask()
      }
    }
    kim.assertCounts(42 to "MC", 0 to "MiningRights_SpecialTile")
  }

  @Test
  internal fun `A steel area cannot grant titanium production from another player's mining tile`() {
    newTestGame()
    stan.exMachina("MiningRights_SpecialTile<Tharsis_8_9>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<MiningRights>>",
            "9 Pay<> FROM MC",
            "MiningRights FROM ProjectCard",
            "MiningRights_SpecialTile<Tharsis_1_1>",
            "2 Steel",
        )
        doTask("PROD[Titanium]")
      }
    }
    kim.assertCounts(42 to "MC", 0 to "MiningRights_SpecialTile", 1 to "PROD[Titanium]")
  }

  @Test
  internal fun `A mixed metal placement grants both resources and steel production`() {
    newTestGame(addOptions = "CimmeriaMap")

    kim.inTurn {
      doTasks("PlayProject<Class<MiningRights>>", "9 Pay<> FROM MC")
      doTasks(
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
          "9 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Cimmeria_6_4>",
          "2 Steel",
          "Titanium",
          "PROD[Titanium]",
      )
    }
  }
}
