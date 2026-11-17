package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AquiferPumpingTest : TfmSandboxTest() {
  @Test
  internal fun `Aquifer Pumping can combine steel with the exact cash remainder`() {
    newTestGame()
    kim.exMachina("$AquiferPumping, $EarthCatapult, 3 Steel")
    kim.setToExMachina(2, "MC")

    kim.inTurn {
      doTasks(
          *cardActionTasks("AquiferPumping"),
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }
  }
}
