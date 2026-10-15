package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SpireTest : TfmSandboxTest() {
  @Test
  internal fun `Counts event tags toward its two-tag threshold`() {
    newTestGame(addOptions = "Spire", kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          doTask("4 ProjectCard")
          doTask("-3 ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<BusinessContacts>>",
              "7 Pay<> FROM MC",
              "BusinessContacts FROM ProjectCard",
          )
          doTask("4 ProjectCard<Kim, Selecting>")
          doTask("2 ProjectCard<Kim, Hand FROM Selecting>")
          doTask("Science<Spire>")
          doTask("PlayedEvent<Class<BusinessContacts>> FROM BusinessContacts")
        }
        .expect("Science<$Spire>")
    kim.inTurn {
          doTasks(
              "PlayProject<Class<MineralDeposit>>",
              "5 Pay<> FROM MC",
              "MineralDeposit FROM ProjectCard",
              "5 Steel",
          )
          doTask("PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit")
        }
        .expect("0 Science<$Spire>")
  }

  @Test
  internal fun `Repeated science tags earn a single resource`() {
    newTestGame(addOptions = "Spire", kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          doTask("4 ProjectCard")
          doTask("-3 ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Research>>",
              "11 Pay<> FROM MC",
              "Research FROM ProjectCard",
          )
          doTask("2 ProjectCard<Kim, Hand>")
          doTask("Science<Spire>")
        }
        .expect("Science<$Spire>")
  }

  @Test
  internal fun `Science pays two MC toward a standard project`() {
    newTestGame(addOptions = "Spire", kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          doTask("4 ProjectCard")
          doTask("-3 ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")

    kim.inTurn {
      doTasks(
          "UseStandardProject<PowerPlantProject>",
          "PayFromCard<Spire> FROM Science<Spire>",
          "9 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }
  }

  @Test
  internal fun `Science cannot pay for project cards`() {
    newTestGame(addOptions = "Spire", kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          doTask("4 ProjectCard")
          doTask("-3 ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks("PlayProject<Class<Mine>>", "PayFromCard<Spire> FROM Science<Spire>")
      }
    }
  }

  @Test
  internal fun `Selling patents does not offer science for the following project payment`() {
    newTestGame(addOptions = "Spire", kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          doTask("4 ProjectCard")
          doTask("-3 ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")
    kim.inTurn {
      doTasks("UseStandardProject<SellPatentsProject>", "1! MC FROM ProjectCard")
    }

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks("PlayProject<Class<Mine>>", "PayFromCard<Spire> FROM Science<Spire>")
      }
    }
  }
}
