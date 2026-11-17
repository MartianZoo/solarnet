package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class WaterImportFromEuropaTest : TfmSandboxTest() {
  @Test
  internal fun `Water Import from Europa can pay its entire action cost in titanium`() {
    newTestGame()
    kim.exMachina("$WaterImportFromEuropa, 4 Titanium")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks(
              *cardActionTasks("WaterImportFromEuropa"),
              "4 Pay<Class<Titanium>> FROM Titanium",
              "Ok", // Nothing remains to pay in MC.
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
              "2 Steel",
          )
        }
        .expect("0 MC")
  }
}
