package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SpireTest : TfmSandboxTest() {
  @Test
  internal fun `Counts event tags toward its two-tag threshold`() {
    newTestGame(kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          selectTask("-3 ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<BusinessContacts>>",
              "-7 MC",
              "BusinessContacts FROM ProjectCard",
          )
          selectTask("4 ProjectCard<Kim, Selecting>!")
          selectTask("2 ProjectCard<Kim, Hand FROM Selecting>!")
          selectTask("-2 ProjectCard<Kim, Selecting>!")
          selectOnlyTask()
          doTask("PlayedEvent<Class<BusinessContacts>> FROM BusinessContacts")
        }
        .expect("Science<$Spire>")
    kim.inTurn {
          doTasks(
              "PlayProject<Class<MineralDeposit>>",
              "-5 MC",
              "MineralDeposit FROM ProjectCard",
              "5 Steel",
          )
          doTask("PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit")
        }
        .expect("0 Science<$Spire>")
  }

  @Test
  internal fun `Repeated science tags earn a single resource`() {
    newTestGame(kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          selectTask("-3 ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks("PlayProject<Class<Research>>", "-11 MC", "Research FROM ProjectCard")
          selectTask("2 ProjectCard<Kim, Hand>!")
          selectOnlyTask()
        }
        .expect("Science<$Spire>")
  }

  @Test
  internal fun `Science pays two MC toward a standard project`() {
    newTestGame(kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          selectTask("-3 ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")

    kim.inTurn {
          doTasks(
              "UseStandardProject<PowerPlantProject>",
              "-Science<Spire>",
              "-MC / Owed",
              "PROD[Energy]",
          )
        }
        .expect("-9 MC")
  }

  @Test
  internal fun `Science cannot pay for project cards`() {
    newTestGame(kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          selectTask("-3 ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")

    shouldThrow<TaskException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<Mine>>", "-Science<Spire>")
      }
    }
  }

  @Test
  internal fun `Selling patents does not offer science for the following project payment`() {
    newTestGame(kimCorporation = Spire)
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          selectTask("-3 ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")
    kim.inTurn {
      doTasks("UseStandardProject<SellPatentsProject>", "1 MC FROM ProjectCard!")
    }

    shouldThrow<TaskException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<Mine>>", "-Science<Spire>")
      }
    }
  }
}
