package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class EcolineTest : TfmSandboxTest() {
  @Test
  internal fun `Can convert seven plants into greenery`() {
    newTestGame(kimCorporation = Ecoline)
    kim.setToExMachina(7, "Plant")

    kim.inTurn {
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTask("GreeneryTile<Tharsis_2_1>")
          selectTask("OxygenStep.")
          doTask("TerraformRating")
        }
        .expect("-7 Plant")
  }
}
