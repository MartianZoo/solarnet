package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SpireTest : TfmSandboxTest() {
  @Test
  internal fun `Counts event tags toward its two-tag threshold`() {
    newTestGame(addOptions = "$Spire", kimCorporation = Spire)
    kim.inTurn {
          doTasks(
              "DoRequiredActions",
              "4 ProjectCard",
              "-3 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks(
              *playCardTasks("$BusinessContacts"),
              "7 Pay<> FROM MC",
              "4 ProjectCard<Selecting>",
              "2 ProjectCard<Hand FROM Selecting>",
              "Science<$Spire>",
          )
        }
        .expect("Science<$Spire>")

    kim.inTurn {
          doTasks(
              *playCardTasks("$MineralDeposit"),
              "5 Pay<> FROM MC",
              "5 Steel",
          )
        }
        .expect("0 Science<$Spire>")
  }

  @Test
  internal fun `Repeated science tags earn a single resource`() {
    newTestGame(addOptions = "$Spire", kimCorporation = Spire)
    kim.inTurn {
          doTasks(
              "DoRequiredActions",
              "4 ProjectCard",
              "-3 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks(
              *playCardTasks("$Research"),
              "11 Pay<> FROM MC",
              "2 ProjectCard<Hand>",
              "Science<$Spire>",
          )
        }
        .expect("Science<$Spire>")
  }

  @Test
  internal fun `Science pays two MC toward a standard project`() {
    newTestGame(addOptions = "$Spire", kimCorporation = Spire)
    kim.inTurn {
          doTasks(
              "DoRequiredActions",
              "4 ProjectCard",
              "-3 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")

    kim.inTurn {
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "PayFromCard<$Spire> FROM Science<$Spire>",
          "9 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }
  }

  @Test
  internal fun `Science cannot pay for project cards`() {
    newTestGame(addOptions = "$Spire", kimCorporation = Spire)
    kim.inTurn {
          doTasks(
              "DoRequiredActions",
              "4 ProjectCard",
              "-3 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*playCardTasks("$Mine"), "PayFromCard<$Spire> FROM Science<$Spire>")
      }
    }
  }

  @Test
  internal fun `Selling patents gains MC without spending science or enabling it for project cards`() {
    newTestGame(addOptions = "$Spire", kimCorporation = Spire)
    kim.inTurn {
          doTasks(
              "DoRequiredActions",
              "4 ProjectCard",
              "-3 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")
    kim.setToExMachina(1, "Science<$Spire>")
    kim.inTurn {
          doTasks(*standardProjectTasks("SellPatentsProject"), "1! MC FROM ProjectCard")
        }
        .expect("0 Science<$Spire>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*playCardTasks("$Mine"), "PayFromCard<$Spire> FROM Science<$Spire>")
      }
    }
  }
}
