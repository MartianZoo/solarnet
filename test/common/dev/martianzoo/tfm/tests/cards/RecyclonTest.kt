package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RecyclonTest : TfmSandboxTest() {
  @Test
  internal fun `Its own building tag adds a starting microbe`() {
    newTestGame(kimCorporation = Recyclon, startAtCorporation = true)

    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
          "Recyclon FROM CorporationCard",
          "38 MC",
          "PROD[Steel]",
          "Microbe<Recyclon>",
          "-30 MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }
  }

  @Test
  internal fun `Gains a microbe when its owner plays a building card`() {
    newTestGame(kimCorporation = Recyclon, startAtCorporation = true)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
          "Recyclon FROM CorporationCard",
          "38 MC",
          "PROD[Steel]",
          "Microbe<Recyclon>",
          "-30 MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "-4 MC")
          selectTask("-X Steel?")
          doTasks("Ok", "Mine FROM ProjectCard", "PROD[Steel]")
          selectOnlyTask()
        }
        .expect("Microbe<$Recyclon>")
  }

  @Test
  internal fun `Converts accumulated microbes including the new building microbe into production`() {
    newTestGame(kimCorporation = Recyclon, startAtCorporation = true)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
          "Recyclon FROM CorporationCard",
          "38 MC",
          "PROD[Steel]",
          "Microbe<Recyclon>",
          "-30 MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }
    advanceUntilActionPhase()
    kim.exMachina("2 Microbe<$Recyclon>")

    kim.inTurn {
      doTasks("PlayProject<Class<TitaniumMine>>", "-7 MC")
      selectTask("-X Steel?")
      doTasks("Ok", "TitaniumMine FROM ProjectCard", "PROD[Titanium]")
      selectOnlyTask()
      doTasks("-2 Microbe THEN PROD[Plant]", "PROD[Plant]")
    }
  }
}
