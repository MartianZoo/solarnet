package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class EcotecTest : TfmSandboxTest() {
  @Test
  internal fun `Rewards both of its own starting tags`() {
    newTestGame(kimCorporation = Ecotec, startAtCorporation = true)

    kim.playCorp(Ecotec) {
          doTask("Plant")
          doTask("Plant")
        }
        .expect("2 Plant")
  }
}
