package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Spire
import dev.martianzoo.tfm.tests.cards.cardnames.StandardTechnology
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class StandardTechnologyTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot fund a standard project with its own rebate`() {
    newTestGame()
    kim.exMachina("$StandardTechnology")
    kim.setToExMachina(8, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseStandardProject<PowerPlantProject>")
        doTask("-11 MC")
      }
    }

    kim.assertCounts(8 to "MC", 1 to "PROD[Energy]", 0 to "Owed", 0 to "Billing")
  }

  @Test
  internal fun `Receives the rebate after paying for a standard project`() {
    newTestGame()
    kim.exMachina("$StandardTechnology")
    kim.setToExMachina(11, "MC")

    kim.inTurn {
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]", "3 MC")
    }
  }

  @Test
  internal fun `Receives no rebate after selling patents`() {
    newTestGame()
    kim.exMachina("$StandardTechnology")

    kim.inTurn { doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard") }
  }

  @Test
  internal fun `Receives a rebate when Spire pays with science after temperature is complete`() {
    newTestGame(addOptions = "Spire", kimCorporation = Spire)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("4 ProjectCard")
      doTask("-3 ProjectCard<Kim, Hand>")
    }
    kim.exMachina("$StandardTechnology")
    kim.setToExMachina(7, "Science<$Spire>")
    stan.setToExMachina(18, "TemperatureStep")
    stan.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }

    kim.inTurn {
          doTasks("UseStandardProject<AsteroidProject>", "-7 Science<Spire>")
          doTask("TemperatureStep")
          doTask("3 MC")
          doTask("Ok" /* No MC payment. */)
        }
        .expect("0 TemperatureStep, 0 TerraformRating")
  }
}
