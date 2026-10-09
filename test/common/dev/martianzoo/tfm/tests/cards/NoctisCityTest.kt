package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class NoctisCityTest : TfmSandboxTest() {
  @Test
  internal fun `Can be placed anywhere on Hellas`() {
    newTestGame(addOptions = "HellasMap")
    kim.setToExMachina(18, "MC")

    kim.playProject(NoctisCity, 18) {
          placeTile(1, 3)
        }
        .expect("PROD[3 MC, -Energy], CityTile")
  }
}
