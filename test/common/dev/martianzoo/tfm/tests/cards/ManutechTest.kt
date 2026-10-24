package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class ManutechTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for its own starting steel production`() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Manutech>>",
          "Manutech FROM CorporationCard",
          "35 MC",
          "PROD[Steel]",
          "Steel",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }
  }

  @Test
  internal fun `Production gains replace the plants spent on Nitrophilic Moss`() {
    newTestGame(kimCorporation = Manutech)
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")
    kim.setToExMachina(2, "Plant")

    kim.inTurn {
          doTasks(
              *playCardTasks("NitrophilicMoss"),
              "8 Pay<> FROM MC",
              "NitrophilicMoss FROM ProjectCard",
              "PROD[2 Plant]",
              "-2 Plant",
              "2 Plant",
          )
        }
        .expect("0 Plant")
  }
}
