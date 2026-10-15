package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RecyclonTest : TfmSandboxTest() {
  @Test
  internal fun `Its own building tag adds a starting microbe`() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
          "Recyclon FROM CorporationCard",
          "38 MC",
          "PROD[Steel]",
          "Microbe<Recyclon>",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }
  }

  @Test
  internal fun `Gains a microbe when its owner plays a building card`() {
    newTestGame(kimCorporation = Recyclon)

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "4 Pay<> FROM MC")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
          doTask("Microbe<Recyclon>")
        }
        .expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Converts accumulated microbes including the new building microbe into production`() {
    newTestGame(kimCorporation = Recyclon)
    kim.exMachina("Microbe<$Recyclon>")

    kim.inTurn {
      doTasks("PlayProject<Class<TitaniumMine>>", "7 Pay<> FROM MC")
      doTasks("TitaniumMine FROM ProjectCard", "PROD[Titanium]")
      doTasks("-2 Microbe THEN PROD[Plant]", "PROD[Plant]")
    }
  }
}
