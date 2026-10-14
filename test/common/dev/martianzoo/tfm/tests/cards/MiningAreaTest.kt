package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.GameplayException
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
      doTasks("PlayProject<Class<MiningArea>>", "-4 MC")
      doTasks("MiningArea FROM ProjectCard", "MiningArea_SpecialTile<Tharsis_8_9>")
      doTasks("Titanium", "PROD[Titanium]")
    }
  }

  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    // Resolved FAQ: copying allows any originally available metal, regardless of the first choice.
    newTestGame(addOptions = "CimmeriaMap")
    kim.exMachina("NormalCityTile<Kim, Cimmeria_5_4>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<$MiningArea>>",
          "-4 MC",
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
          "-9 MC",
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

    shouldThrow<GameplayException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<MiningArea>>", "-4 MC")
        selectTask("-X? Steel")
        selectTask("Ok")
        doTask("MiningArea FROM ProjectCard")
        selectOnlyTask()
        doTask("MiningArea_SpecialTile<Tharsis_3_2>")
        selectOnlyTask()
      }
    }
  }
}
