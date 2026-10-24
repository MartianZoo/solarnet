package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class NoctisCityTest : TfmSandboxTest() {
  @Test
  internal fun `Places its city on the reserved Tharsis area`() {
    newTestGame()
    kim.setToExMachina(18, "MC")

    kim.inTurn {
      doTasks(*playCardTasks("NoctisCity"), "18 Pay<> FROM MC")
      doTasks(
          "NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Tharsis_5_3>",
          "2 Plant",
      )
    }
  }

  @Test
  internal fun `Can be placed anywhere on Hellas`() {
    newTestGame(addOptions = "HellasMap")
    kim.setToExMachina(18, "MC")

    kim.inTurn {
      doTasks(*playCardTasks("NoctisCity"), "18 Pay<> FROM MC")
      doTasks(
          "NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_1_3>",
          "2 Plant",
      )
    }
  }
}
