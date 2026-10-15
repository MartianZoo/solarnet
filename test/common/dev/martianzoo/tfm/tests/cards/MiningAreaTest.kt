package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class MiningAreaTest : TfmSandboxTest() {
  @Test
  internal fun `Can be placed adjacent to a titanium area`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Kim, Tharsis_7_9>")

    kim.inTurn {
      doTasks("PlayProject<Class<MiningArea>>", "4 Pay<> FROM MC")
      doTasks(
          "MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Tharsis_8_9>",
      )
      doTasks("Titanium", "PROD[Titanium]")
    }
  }

  // Unresolved adjudication (question 21, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/2663453
  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    newTestGame(addOptions = "CimmeriaMap")
    kim.exMachina("NormalCityTile<Kim, Cimmeria_5_4>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<$MiningArea>>",
          "4 Pay<> FROM MC",
          "$MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Cimmeria_6_4>",
          "2 Steel",
          "Titanium",
          "PROD[Steel]",
      )
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<$RoboticWorkforce>>",
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<$MiningArea>",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cannot select an area without a metal placement bonus`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Kim, Tharsis_2_1>")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("PlayProject<Class<MiningArea>>", "4 Pay<> FROM MC")
        doTask("MiningArea FROM ProjectCard")
        doTask("MiningArea_SpecialTile<Tharsis_3_2>")
        // Resolve the production choice to check that this area supplies a metal bonus.
        selectOnlyTask()
      }
    }
    kim.assertCounts(42 to "MC", 0 to "MiningArea_SpecialTile")
  }

  @Test
  internal fun `Another player's adjacent tile does not permit placement`() {
    newTestGame()
    stan.exMachina("NormalCityTile<Tharsis_7_9>")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("PlayProject<Class<MiningArea>>", "4 Pay<> FROM MC", "MiningArea FROM ProjectCard")
        doTask("MiningArea_SpecialTile<Tharsis_8_9>")
      }
    }
    kim.assertCounts(42 to "MC", 0 to "MiningArea_SpecialTile")
  }

  @Test
  internal fun `A titanium area cannot grant steel production from another player's mining tile`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Tharsis_7_9>")
    stan.exMachina("MiningArea_SpecialTile<Tharsis_1_1>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<MiningArea>>",
            "4 Pay<> FROM MC",
            "MiningArea FROM ProjectCard",
            "MiningArea_SpecialTile<Tharsis_8_9>",
            "Titanium",
        )
        doTask("PROD[Steel]")
      }
    }
    kim.assertCounts(42 to "MC", 0 to "MiningArea_SpecialTile", 1 to "PROD[Steel]")
  }
}
