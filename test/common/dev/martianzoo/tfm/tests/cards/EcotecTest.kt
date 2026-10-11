package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class EcotecTest : TfmSandboxTest() {
  @Test
  internal fun `Rewards both of its own starting tags`() {
    newTestGame(kimCorporation = Ecotec, startAtCorporation = true)

    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Ecotec>>",
          "Ecotec FROM CorporationCard",
          "42 MC",
          "PROD[Plant]",
          "Plant",
          "Plant",
          "-30 MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }
  }
}
