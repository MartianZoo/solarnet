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

    shouldThrow<LimitsException> { kim.stdProject("PowerPlantProject") }

    kim.assertCounts(8 to "MC", 1 to "PROD[Energy]", 0 to "Owed", 0 to "ActionBilling")
  }

  @Test
  internal fun `Receives the rebate after paying for a standard project`() {
    newTestGame()
    kim.exMachina("$StandardTechnology")
    kim.setToExMachina(11, "MC")

    kim.stdProject("PowerPlantProject").expect("-8 MC, PROD[Energy]")
  }

  @Test
  internal fun `Receives no rebate after selling patents`() {
    newTestGame()
    kim.exMachina("$StandardTechnology")

    kim.sellPatents(1).expect("MC")
  }

  @Test
  internal fun `Receives a rebate when Spire pays with science after temperature is complete`() {
    newTestGame(kimCorporation = Spire)
    kim.stdAction("DoRequiredActionsAction")
    kim.exMachina("$StandardTechnology")
    kim.setToExMachina(7, "Science<$Spire>")
    stan.setToExMachina(18, "TemperatureStep")
    stan.stdProject("AsteroidProject")

    kim.stdProject(
            "AsteroidProject",
            payment = {
              doTask("-7 Science<$Spire>")
              declineTask()
            },
        )
        .expect("3 MC, -7 Science<$Spire>, 0 TemperatureStep, 0 TerraformRating")
  }
}
