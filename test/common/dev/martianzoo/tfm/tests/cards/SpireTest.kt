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
    kim.stdAction("DoRequiredActionsAction").expect("ProjectCard")

    kim.playProject(BusinessContacts, 7).expect("Science<$Spire>")
    kim.playProject(MineralDeposit, 5).expect("0 Science<$Spire>")
  }

  @Test
  internal fun `Repeated science tags earn a single resource`() {
    newTestGame(kimCorporation = Spire)
    kim.stdAction("DoRequiredActionsAction").expect("ProjectCard")

    kim.playProject(Research, 11).expect("Science<$Spire>")
  }

  @Test
  internal fun `Science pays two MC toward a standard project`() {
    newTestGame(kimCorporation = Spire)
    kim.stdAction("DoRequiredActionsAction").expect("ProjectCard")

    kim.stdProject(
            "PowerPlantProject",
            payment = {
              doTask("-Science<$Spire>")
              doTask("-MC / Owed")
            },
        )
        .expect("-Science<$Spire>, -9 MC, PROD[Energy]")
  }

  @Test
  internal fun `Science cannot pay for project cards`() {
    newTestGame(kimCorporation = Spire)
    kim.stdAction("DoRequiredActionsAction").expect("ProjectCard")

    shouldThrow<TaskException> {
      kim.playProject(Mine, payment = { doTask("-Science<$Spire>") })
    }
  }

  @Test
  internal fun `Selling patents does not offer science for the following project payment`() {
    newTestGame(kimCorporation = Spire)
    kim.stdAction("DoRequiredActionsAction").expect("ProjectCard")
    kim.sellPatents(1).expect("MC, -ProjectCard")

    shouldThrow<TaskException> {
      kim.playProject(Mine, payment = { doTask("-Science<$Spire>") })
    }
  }
}
