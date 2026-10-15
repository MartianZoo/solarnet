package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class NoctisCityTest : TfmSandboxTest() {
  @Test
  internal fun `Can be placed anywhere on Hellas`() {
    newTestGame(addOptions = "HellasMap")
    kim.setToExMachina(18, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<NoctisCity>>", "-18 MC")
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
